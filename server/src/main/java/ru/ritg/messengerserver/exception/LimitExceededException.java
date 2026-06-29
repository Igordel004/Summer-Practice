package ru.ritg.messengerserver.exception;

/**
 * Исключение: превышен лимит соединений или размера данных.
 *
 * <p>Выбрасывается при превышении лимита WebSocket-соединений (500)
 * или при отправке сообщения, превышающего 4096 байт.</p>
 *
 * <p>HTTP-статус: 503 Service Unavailable.</p>
 */
public class LimitExceededException extends RuntimeException {

    public LimitExceededException(String message) {
        super(message);
    }
}
