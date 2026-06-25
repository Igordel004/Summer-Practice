package ru.ritg.gsmessage.service;

import org.springframework.stereotype.Service;
import ru.ritg.gsmessage.model.Message;
import ru.ritg.gsmessage.model.MessageStatus;

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
    public Message routeMessage(UUID senderId, UUID recipientId, String payload) {
        throw new UnsupportedOperationException("Not implemented yet");
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
    public void updateStatus(UUID messageId, MessageStatus status) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Получить недоставленные сообщения для пользователя (буферизация).
     *
     * @param userId идентификатор пользователя
     * @return список сообщений со статусом PENDING
     */
    public List<Message> getBufferedMessages(UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Получить историю переписки с пагинацией.
     *
     * @param userId идентификатор пользователя
     * @param offset смещение
     * @param limit  максимальное количество записей
     * @return список сообщений
     */
    public List<Message> getHistory(UUID userId, int offset, int limit) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
