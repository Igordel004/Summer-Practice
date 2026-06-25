package ru.ritg.gsmessage.exception;

/**
 * Исключение: превышен лимит соединений.
 *
 * <p>Выбрасывается при попытке установить WebSocket-соединение,
 * когда количество активных подключений достигло максимума (500).</p>
 *
 * <p>HTTP-статус: 503 Service Unavailable.</p>
 */
public class LimitExceededException extends RuntimeException {

    public LimitExceededException(String message) {
        super(message);
    }
}
