package ru.ritg.messengerserver.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.ritg.messengerserver.security.JwtTokenProvider;

import java.util.Map;

/**
 * Interceptor для WebSocket handshake.
 *
 * <p>Извлекает JWT-токен из query-параметров или заголовков запроса
 * и валидирует его перед установлением соединения.</p>
 */
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);
    private static final String TOKEN_PARAM = "token";
    private static final int MAX_CONNECTIONS = 500;

    private final JwtTokenProvider jwtTokenProvider;
    private final SessionRegistry sessionRegistry;

    public WebSocketHandshakeInterceptor(JwtTokenProvider jwtTokenProvider,
                                         SessionRegistry sessionRegistry) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionRegistry = sessionRegistry;
    }

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
        String remoteAddr = request.getRemoteAddress() != null
                ? request.getRemoteAddress().toString() : "unknown";

        if (sessionRegistry.getAllSessions().size() >= MAX_CONNECTIONS) {
            log.warn("WebSocket handshake rejected (503): connection limit {} exceeded, ip={}", MAX_CONNECTIONS, remoteAddr);
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }

        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket handshake rejected (1008): invalid or missing token, ip={}", remoteAddr);
            return false;
        }

        String userId = jwtTokenProvider.getUserIdFromToken(token).toString();
        attributes.put("userId", userId);
        attributes.put("token", token);
        log.info("WebSocket handshake accepted: user={}, ip={}", userId, remoteAddr);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String tokenParam = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
            if (tokenParam != null && !tokenParam.isEmpty()) {
                return tokenParam;
            }

            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }
}
