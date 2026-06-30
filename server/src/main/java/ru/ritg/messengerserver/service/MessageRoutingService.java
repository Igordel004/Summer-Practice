package ru.ritg.messengerserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ritg.messengerserver.exception.LimitExceededException;
import ru.ritg.messengerserver.exception.MessageNotFoundException;
import ru.ritg.messengerserver.model.Message;
import ru.ritg.messengerserver.model.MessageStatus;
import ru.ritg.messengerserver.model.User;
import ru.ritg.messengerserver.repository.MessageRepository;
import ru.ritg.messengerserver.repository.UserRepository;

import org.springframework.data.domain.PageRequest;
import java.util.Collections;

import java.util.List;
import java.util.UUID;

/**
 * Сервис маршрутизации сообщений.
 *
 * <p>Отвечает за доставку сообщений в реальном времени (WebSocket)
 * и буферизацию для офлайн-пользователей.</p>
 */
@Service
public class MessageRoutingService {

    private static final Logger log = LoggerFactory.getLogger(MessageRoutingService.class);
    private static final int MAX_PAYLOAD_LENGTH = 4096;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageRoutingService(MessageRepository messageRepository,
                                 UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Маршрутизировать сообщение от отправителя к получателю.
     *
     * <p>Входные данные: идентификаторы отправителя/получателя и текст.</p>
     * <p>Параметры:</p>
     * <ul>
     *   <li>{@code senderId} — UUID отправителя, обязательный</li>
     *   <li>{@code recipientId} — UUID получателя, обязательный</li>
     *   <li>{@code payload} — текст сообщения, обязательный, ≤ 4096 символов</li>
     * </ul>
     * <p>Ожидаемый результат: {@link Message} с {@code status=PENDING} (или DELIVERED, если онлайн).</p>
     * <p>Возможные ошибки:</p>
     * <ul>
     *   <li>{@code UnauthorizedException} — отправитель не авторизован</li>
     *   <li>{@code MessageTooLongException} — payload превышает 4096 символов</li>
     * </ul>
     *
     * @param senderId    идентификатор отправителя
     * @param recipientId идентификатор получателя
     * @param payload     текст сообщения
     * @return созданное сообщение
     */
    @Transactional
    public Message routeMessage(UUID senderId, UUID recipientId, String payload) {
        if (payload != null && payload.length() > MAX_PAYLOAD_LENGTH) {
            throw new LimitExceededException("Payload exceeds " + MAX_PAYLOAD_LENGTH + " bytes");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new MessageNotFoundException(null));

        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .payload(payload)
                .status(MessageStatus.PENDING)
                .build();

        message = messageRepository.save(message);
        log.info("Message {} saved with status PENDING", message.getId());
        return message;
    }

    /**
     * Обновить статус доставки сообщения.
     *
     * <p>Входные данные: идентификатор сообщения и новый статус.</p>
     * <p>Параметры:</p>
     * <ul>
     *   <li>{@code messageId} — UUID сообщения, обязательный</li>
     *   <li>{@code status} — новый статус (DELIVERED или READ)</li>
     * </ul>
     * <p>Ожидаемый результат: статус обновлён в БД, уведомление отправлено через WebSocket.</p>
     * <p>Возможные ошибки:</p>
     * <ul>
     *   <li>{@code MessageNotFoundException} — сообщение не найдено</li>
     * </ul>
     *
     * @param messageId идентификатор сообщения
     * @param status    новый статус
     */
    @Transactional
    public Message updateStatus(UUID messageId, MessageStatus status) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
        message.setStatus(status);
        messageRepository.save(message);
        log.info("Message {} status updated to {}", messageId, status);
        return message;
    }

    /**
     * Получить недоставленные сообщения для пользователя (буферизация).
     *
     * @param userId идентификатор пользователя
     * @return список сообщений со статусом PENDING
     */
    @Transactional(readOnly = true)
    public List<Message> getBufferedMessages(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        return messageRepository.findByRecipientAndStatus(user, MessageStatus.PENDING);
    }

    /**
     * Получить сообщения отправителя с изменённым статусом (DELIVERED / READ).
     *
     * <p>Используется при подключении пользователя через WebSocket для отправки
     * уведомлений ({@code status_ack}) отправителю о статусе его сообщений,
     * которые были доставлены или прочитаны пока он был офлайн.</p>
     *
     * @param userId UUID отправителя
     * @return список сообщений (не более 100 последних) со статусами DELIVERED или READ
     */
    @Transactional(readOnly = true)
    public List<Message> getStatusUpdatesForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        return messageRepository.findBySenderAndStatusIn(user,
                java.util.List.of(MessageStatus.DELIVERED, MessageStatus.READ),
                org.springframework.data.domain.PageRequest.of(0, 100));
    }

    /**
     * Получить список пользователей, с которыми есть переписка.
     *
     * @param userId идентификатор пользователя
     * @return список собеседников
     */
    @Transactional(readOnly = true)
    public List<User> getChats(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        java.util.Set<User> partners = new java.util.LinkedHashSet<>();
        partners.addAll(messageRepository.findRecipientsBySender(user));
        partners.addAll(messageRepository.findSendersByRecipient(user));
        return new java.util.ArrayList<>(partners);
    }

    /**
     * Получить историю переписки с пагинацией.
     *
     * @param userId    идентификатор пользователя
     * @param partnerId идентификатор собеседника
     * @param offset    смещение
     * @param limit     максимальное количество записей
     * @return список сообщений
     */
    @Transactional(readOnly = true)
    public List<Message> getHistory(UUID userId, UUID partnerId, int offset, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        User contact = userRepository.findById(partnerId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        List<Message> result = messageRepository.findConversationPaginated(user, contact,
                PageRequest.of(offset / Math.max(limit, 1), Math.max(limit, 1)));
        Collections.reverse(result);
        return result;
    }

    /**
     * Подсчитать общее количество сообщений в диалоге.
     *
     * @param userId    UUID пользователя
     * @param partnerId UUID собеседника
     * @return общее число сообщений
     */
    @Transactional(readOnly = true)
    public long countHistory(UUID userId, UUID partnerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        User contact = userRepository.findById(partnerId)
                .orElseThrow(() -> new MessageNotFoundException(null));
        long count = messageRepository.countConversation(user, contact);
        return count;
    }

    /**
     * Отредактировать текст сообщения.
     *
     * <p>Редактирование разрешено только автору сообщения.</p>
     *
     * @param messageId  UUID сообщения
     * @param userId     UUID пользователя (автор)
     * @param newPayload новый текст
     * @return обновлённое сообщение
     */
    @Transactional
    public Message editMessage(UUID messageId, UUID userId, String newPayload) {
        if (newPayload != null && newPayload.length() > MAX_PAYLOAD_LENGTH) {
            throw new LimitExceededException("Payload exceeds " + MAX_PAYLOAD_LENGTH + " bytes");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
        if (!message.getSender().getId().equals(userId)) {
            throw new SecurityException("Only sender can edit own messages");
        }
        message.setPayload(newPayload);
        messageRepository.save(message);
        log.info("Message {} edited by user {}", messageId, userId);
        return message;
    }

    /**
     * Удалить сообщение.
     *
     * <p>Удаление разрешено только автору сообщения.</p>
     *
     * @param messageId UUID сообщения
     * @param userId    UUID пользователя (автор)
     * @return удалённое сообщение (перед удалением)
     */
    @Transactional
    public Message deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
        if (!message.getSender().getId().equals(userId)) {
            throw new SecurityException("Only sender can delete own messages");
        }
        messageRepository.deleteById(messageId);
        log.info("Message {} deleted by user {}", messageId, userId);
        return message;
    }
}
