package ru.ritg.messengerserver.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реестр активных WebSocket-сессий.
 *
 * <p>Хранит соответствие userId → WebSocketSession в {@link ConcurrentHashMap}.
 * Используется для маршрутизации сообщений и проверки онлайн-статуса.</p>
 */
@Component
public class SessionRegistry {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Зарегистрировать сессию пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param session WebSocket-сессия
     */
    public void register(String userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    /**
     * Удалить сессию пользователя.
     *
     * @param userId идентификатор пользователя
     */
    public void unregister(String userId) {
        sessions.remove(userId);
    }

    /**
     * Получить сессию по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return {@link Optional} с сессией
     */
    public Optional<WebSocketSession> getSession(String userId) {
        return Optional.ofNullable(sessions.get(userId));
    }

    /**
     * Проверить, находится ли пользователь онлайн.
     *
     * @param userId идентификатор пользователя
     * @return {@code true}, если сессия активна
     */
    public boolean isOnline(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * Получить все активные сессии.
     *
     * @return коллекция WebSocket-сессий
     */
    public Collection<WebSocketSession> getAllSessions() {
        return sessions.values();
    }
}
