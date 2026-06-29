package ru.ritg.messengerserver.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.ritg.messengerserver.dto.ws.WsDelivery;
import ru.ritg.messengerserver.dto.ws.WsMessage;
import ru.ritg.messengerserver.dto.ws.WsStatusUpdate;
import ru.ritg.messengerserver.model.Message;
import ru.ritg.messengerserver.model.MessageStatus;
import ru.ritg.messengerserver.security.JwtTokenProvider;
import ru.ritg.messengerserver.service.AuthService;
import ru.ritg.messengerserver.service.MessageRoutingService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * WebSocket-обработчик входящих сообщений.
 *
 * <p>Управляет жизненным циклом WebSocket-соединения:</p>
 * <ul>
 *   <li>Handshake с валидацией JWT</li>
 *   <li>Приём JSON-сообщений (WsMessage, WsStatusUpdate, edit, delete)</li>
 *   <li>Доставка/буферизация исходящих сообщений</li>
 *   <li>Редактирование и удаление сообщений</li>
 * </ul>
 */
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageWebSocketHandler.class);
    private static final String TYPE_MESSAGE = "message";
    private static final String TYPE_STATUS = "status";
    private static final String TYPE_EDIT = "edit";
    private static final String TYPE_DELETE = "delete";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JwtTokenProvider jwtTokenProvider;
    private final MessageRoutingService messageRoutingService;
    private final SessionRegistry sessionRegistry;
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public MessageWebSocketHandler(JwtTokenProvider jwtTokenProvider,
                                   MessageRoutingService messageRoutingService,
                                   SessionRegistry sessionRegistry,
                                   AuthService authService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.messageRoutingService = messageRoutingService;
        this.sessionRegistry = sessionRegistry;
        this.authService = authService;
    }

    /**
     * Обработка установления соединения.
     *
     * <p>Входные данные: WebSocket handshake с JWT в query-параметре.</p>
     * <p>Параметры: {@code token} (String) — обязательный JWT.</p>
     * <p>Ожидаемый результат: сессия зарегистрирована в {@link SessionRegistry}.</p>
     * <p>Возможные ошибки: 401 Policy Violation (невалидный токен), 503 (лимит 500).</p>
     *
     * @param session WebSocket-сессия
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            log.warn("No userId in session attributes, closing connection");
            try {
                session.close(new CloseStatus(4001, "Missing user ID"));
            } catch (IOException e) {
                log.error("Error closing session", e);
            }
            return;
        }

        sessionRegistry.register(userId, session);
        String remoteAddr = session.getRemoteAddress() != null
                ? session.getRemoteAddress().toString() : "unknown";
        log.info("WebSocket connected: user={}, ip={}", userId, remoteAddr);

        try {
            var buffered = messageRoutingService.getBufferedMessages(UUID.fromString(userId));
            for (Message msg : buffered) {
                WsDelivery delivery = new WsDelivery();
                delivery.setType("delivery");
                delivery.setMessageId(msg.getId());
                delivery.setSenderId(msg.getSender().getId());
                delivery.setRecipientId(msg.getRecipient().getId());
                delivery.setPayload(msg.getPayload());
                delivery.setStatus(MessageStatus.DELIVERED);
                delivery.setTimestamp(LocalDateTime.now().format(TS_FORMAT));

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(delivery)));
                messageRoutingService.updateStatus(msg.getId(), MessageStatus.DELIVERED);
            }
            if (!buffered.isEmpty()) {
                log.info("Delivered {} buffered messages to user {}", buffered.size(), userId);
            }
        } catch (IOException e) {
            log.error("Error delivering buffered messages", e);
        }
    }

    /**
     * Обработка входящего текстового сообщения.
     *
     * <p>Поддерживаемые типы JSON:</p>
     * <ul>
     *   <li>{@code message} — отправка сообщения</li>
     *   <li>{@code status} — обновление статуса (READ)</li>
     *   <li>{@code edit} — редактирование сообщения</li>
     *   <li>{@code delete} — удаление сообщения</li>
     * </ul>
     *
     * @param session WebSocket-сессия
     * @param message входящее текстовое сообщение
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            var node = objectMapper.readTree(payload);
            String type = node.has("type") ? node.get("type").asText() : "";

            if (TYPE_MESSAGE.equals(type)) {
                WsMessage wsMessage = objectMapper.treeToValue(node, WsMessage.class);
                handleMessage(session, wsMessage);
            } else if (TYPE_STATUS.equals(type)) {
                WsStatusUpdate wsStatus = objectMapper.treeToValue(node, WsStatusUpdate.class);
                handleStatusUpdate(session, wsStatus);
            } else if (TYPE_EDIT.equals(type)) {
                handleEdit(session, node);
            } else if (TYPE_DELETE.equals(type)) {
                handleDelete(session, node);
            } else {
                log.warn("Unknown message type: {}", type);
                session.close(new CloseStatus(1008, "Unknown message type"));
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            try {
                session.close(new CloseStatus(1008, "Invalid message format"));
            } catch (IOException ex) {
                log.error("Error closing session", ex);
            }
        }
    }

    /**
     * Обработка закрытия соединения.
     *
     * @param session WebSocket-сессия
     * @param status  статус закрытия
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            sessionRegistry.unregister(userId);
            String remoteAddr = session.getRemoteAddress() != null
                    ? session.getRemoteAddress().toString() : "unknown";
            log.info("WebSocket disconnected: user={}, status={}, ip={}", userId, status, remoteAddr);
        }
    }

    private void handleMessage(WebSocketSession senderSession, WsMessage wsMessage) {
        try {
            Message message = messageRoutingService.routeMessage(
                    wsMessage.getSenderId(),
                    wsMessage.getRecipientId(),
                    wsMessage.getPayload()
            );

            MessageStatus ackStatus = message.getStatus();
            String recipientId = wsMessage.getRecipientId().toString();
            if (sessionRegistry.isOnline(recipientId)) {
                var recipientSession = sessionRegistry.getSession(recipientId);
                if (recipientSession.isPresent() && recipientSession.get().isOpen()) {
                    WsDelivery delivery = new WsDelivery();
                    delivery.setType("delivery");
                    delivery.setMessageId(message.getId());
                    delivery.setSenderId(wsMessage.getSenderId());
                    delivery.setRecipientId(wsMessage.getRecipientId());
                    delivery.setPayload(wsMessage.getPayload());
                    delivery.setStatus(MessageStatus.DELIVERED);
                    delivery.setTimestamp(LocalDateTime.now().format(TS_FORMAT));

                    recipientSession.get().sendMessage(
                            new TextMessage(objectMapper.writeValueAsString(delivery)));
                    messageRoutingService.updateStatus(message.getId(), MessageStatus.DELIVERED);
                    ackStatus = MessageStatus.DELIVERED;
                }
            }

            WsDelivery ack = new WsDelivery();
            ack.setType("ack");
            ack.setMessageId(message.getId());
            ack.setSenderId(wsMessage.getSenderId());
            ack.setRecipientId(wsMessage.getRecipientId());
            ack.setPayload(wsMessage.getPayload());
            ack.setStatus(ackStatus);
            ack.setTimestamp(LocalDateTime.now().format(TS_FORMAT));

            senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
        } catch (Exception e) {
            log.error("Error routing message", e);
        }
    }

    private void handleStatusUpdate(WebSocketSession session, WsStatusUpdate wsStatus) {
        try {
            Message message = messageRoutingService.updateStatus(wsStatus.getMessageId(), wsStatus.getStatus());

            String senderId = message.getSender().getId().toString();
            if (sessionRegistry.isOnline(senderId)) {
                var senderSession = sessionRegistry.getSession(senderId);
                if (senderSession.isPresent() && senderSession.get().isOpen()) {
                    WsStatusUpdate notify = new WsStatusUpdate();
                    notify.setType("status_ack");
                    notify.setMessageId(wsStatus.getMessageId());
                    notify.setStatus(wsStatus.getStatus());
                    notify.setTimestamp(LocalDateTime.now().format(TS_FORMAT));

                    senderSession.get().sendMessage(
                            new TextMessage(objectMapper.writeValueAsString(notify)));
                }
            }
        } catch (Exception e) {
            log.error("Error updating message status", e);
        }
    }

    /**
     * Обработка редактирования сообщения.
     *
     * <p>Формат входного JSON:</p>
     * <pre>{"type":"edit","messageId":"uuid","newPayload":"новый текст"}</pre>
     *
     * <p>Разрешено только автору сообщения. После обновления в БД:</p>
     * <ul>
     *   <li>Отправителю — {@code edit_ack} с обновлённым текстом</li>
     *   <li>Получателю (если онлайн) — {@code edit_notify}</li>
     * </ul>
     *
     * @param session WebSocket-сессия отправителя
     * @param node    JSON-узел с данными
     */
    private void handleEdit(WebSocketSession session, com.fasterxml.jackson.databind.JsonNode node) {
        try {
            String userId = (String) session.getAttributes().get("userId");
            UUID messageId = UUID.fromString(node.get("messageId").asText());
            String newPayload = node.get("newPayload").asText();

            Message message = messageRoutingService.editMessage(messageId, UUID.fromString(userId), newPayload);

            java.util.Map<String, Object> editAck = new java.util.LinkedHashMap<>();
            editAck.put("type", "edit_ack");
            editAck.put("messageId", message.getId().toString());
            editAck.put("newPayload", message.getPayload());
            editAck.put("timestamp", LocalDateTime.now().format(TS_FORMAT));

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(editAck)));

            String recipientId = message.getRecipient().getId().toString();
            if (sessionRegistry.isOnline(recipientId)) {
                var recipientSession = sessionRegistry.getSession(recipientId);
                if (recipientSession.isPresent() && recipientSession.get().isOpen()) {
                    java.util.Map<String, Object> editNotify = new java.util.LinkedHashMap<>();
                    editNotify.put("type", "edit_notify");
                    editNotify.put("messageId", message.getId().toString());
                    editNotify.put("senderId", userId);
                    editNotify.put("newPayload", message.getPayload());
                    editNotify.put("timestamp", LocalDateTime.now().format(TS_FORMAT));

                    recipientSession.get().sendMessage(
                            new TextMessage(objectMapper.writeValueAsString(editNotify)));
                }
            }
        } catch (Exception e) {
            log.error("Error editing message", e);
        }
    }

    /**
     * Обработка удаления сообщения.
     *
     * <p>Формат входного JSON:</p>
     * <pre>{"type":"delete","messageId":"uuid"}</pre>
     *
     * <p>Разрешено только автору сообщения. После удаления из БД:</p>
     * <ul>
     *   <li>Отправителю — {@code delete_ack}</li>
     *   <li>Получателю (если онлайн) — {@code delete_notify}</li>
     * </ul>
     *
     * @param session WebSocket-сессия отправителя
     * @param node    JSON-узел с данными
     */
    private void handleDelete(WebSocketSession session, com.fasterxml.jackson.databind.JsonNode node) {
        try {
            String userId = (String) session.getAttributes().get("userId");
            UUID messageId = UUID.fromString(node.get("messageId").asText());

            Message message = messageRoutingService.deleteMessage(messageId, UUID.fromString(userId));

            java.util.Map<String, Object> deleteAck = new java.util.LinkedHashMap<>();
            deleteAck.put("type", "delete_ack");
            deleteAck.put("messageId", messageId.toString());
            deleteAck.put("timestamp", LocalDateTime.now().format(TS_FORMAT));

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(deleteAck)));

            String recipientId = message.getRecipient().getId().toString();
            if (sessionRegistry.isOnline(recipientId)) {
                var recipientSession = sessionRegistry.getSession(recipientId);
                if (recipientSession.isPresent() && recipientSession.get().isOpen()) {
                    java.util.Map<String, Object> deleteNotify = new java.util.LinkedHashMap<>();
                    deleteNotify.put("type", "delete_notify");
                    deleteNotify.put("messageId", messageId.toString());
                    deleteNotify.put("senderId", userId);
                    deleteNotify.put("timestamp", LocalDateTime.now().format(TS_FORMAT));

                    recipientSession.get().sendMessage(
                            new TextMessage(objectMapper.writeValueAsString(deleteNotify)));
                }
            }
        } catch (Exception e) {
            log.error("Error deleting message", e);
        }
    }
}
