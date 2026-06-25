package ru.ritg.gsmessage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Конфигурация WebSocket.
 *
 * <p>Регистрирует WebSocket-обработчик на порту 8081, путь /ws/messages.</p>
 */
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
