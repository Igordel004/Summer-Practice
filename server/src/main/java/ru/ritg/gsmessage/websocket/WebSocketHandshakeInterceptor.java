package ru.ritg.gsmessage.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor для WebSocket handshake.
 *
 * <p>Извлекает JWT-токен из query-параметров или заголовков запроса
 * и валидирует его перед установлением соединения.</p>
 */
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * Выполнить проверки перед handshake.
     *
     * <p>Входные данные: HTTP-запрос с JWT в query или header.</p>
     * <p>Ожидаемый результат: {@code true} — handshake разрешён, {@code false} — отклонён.</p>
     *
     * @param request   HTTP-запрос
     * @param response  HTTP-ответ
     * @param handler   WebSocket-обработчик
     * @param attributes атрибуты сессии
     * @return флаг разрешения handshake
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        // no-op
    }
}
