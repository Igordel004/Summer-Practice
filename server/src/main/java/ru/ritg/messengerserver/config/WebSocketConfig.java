package ru.ritg.messengerserver.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import ru.ritg.messengerserver.websocket.MessageWebSocketHandler;
import ru.ritg.messengerserver.websocket.WebSocketHandshakeInterceptor;

/**
 * Конфигурация WebSocket.
 *
 * <p>Регистрирует WebSocket-обработчик на порту 8081, путь /ws/messages.</p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final int WS_PORT = 8081;
    private static final int MAX_MESSAGE_SIZE = 4096;
    private static final long MAX_SESSION_IDLE_TIMEOUT = 300000L;

    private final MessageWebSocketHandler messageWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(MessageWebSocketHandler messageWebSocketHandler,
                           WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    /**
     * Регистрация WebSocket-обработчика и перехватчика handshake.
     *
     * <p>Входные данные: {@link MessageWebSocketHandler} и {@link WebSocketHandshakeInterceptor},
     * внедрённые через конструктор.</p>
     * <p>Параметры: {@code registry} — реестр обработчиков WebSocket-соединений.</p>
     * <p>Ожидаемый результат: обработчик {@code /ws/messages} зарегистрирован с перехватчиком
     * авторизации JWT, разрешены запросы с любых источников ({@code setAllowedOrigins("*")}).</p>
     * <p>Возможные ошибки: нет (конфигурационный метод).</p>
     *
     * @param registry реестр WebSocket-обработчиков
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/ws/messages")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * Конфигурация WebSocket-контейнера.
     *
     * <p>Входные данные: нет.</p>
     * <p>Параметры: нет.</p>
     * <p>Ожидаемый результат: настроенный {@link ServletServerContainerFactoryBean} с лимитами размера
     * текстовых и бинарных сообщений, а также таймаутом неактивной сессии.</p>
     * <p>Возможные ошибки: нет (конфигурационный метод).</p>
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketServerFactory() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(MAX_MESSAGE_SIZE);
        container.setMaxBinaryMessageBufferSize(MAX_MESSAGE_SIZE);
        container.setMaxSessionIdleTimeout(MAX_SESSION_IDLE_TIMEOUT);
        return container;
    }

    /**
     * Дополнительный Tomcat-коннектор для WebSocket на порту 8081.
     *
     * <p>Входные данные: нет.</p>
     * <p>Параметры: нет.</p>
     * <p>Ожидаемый результат: добавлен {@link Connector} на порту 8081 в фабрику веб-сервера Tomcat.</p>
     * <p>Возможные ошибки: занятый порт 8081 при старте приложения.</p>
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatConnectorCustomizer() {
        return factory -> {
            Connector wsConnector = new Connector();
            wsConnector.setPort(WS_PORT);
            factory.addAdditionalTomcatConnectors(wsConnector);
        };
    }
}
