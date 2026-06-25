package ru.ritg.gsmessage.exception;

/**
 * Исключение: неавторизованный доступ.
 *
 * <p>Выбрасывается при отсутствии или невалидности JWT-токена
 * в SOAP-заголовке или WebSocket handshake.</p>
 *
 * <p>HTTP-статус: 401 Unauthorized.</p>
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String token) {
        super("Unauthorized: invalid or missing token");
    }
}
