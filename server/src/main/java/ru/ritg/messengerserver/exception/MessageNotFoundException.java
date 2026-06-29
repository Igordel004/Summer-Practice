package ru.ritg.messengerserver.exception;

import java.util.UUID;

/**
 * Исключение: сообщение не найдено.
 *
 * <p>Выбрасывается при попытке обновить статус несуществующего
 * сообщения или при ответе на удалённое сообщение.</p>
 *
 * <p>SOAP Fault: {@code MessageNotFound}.</p>
 */
public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(UUID messageId) {
        super("Message not found: " + messageId);
    }
}
