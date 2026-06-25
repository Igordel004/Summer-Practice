package ru.ritg.gsmessage.security;

import ru.ritg.gsmessage.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Провайдер JWT-токенов.
 *
 * <p>Отвечает за генерацию, валидацию и разбор JWT-токенов сессий.
 * Алгоритм подписи: HMAC SHA-256. Срок действия: 24 часа.</p>
 */
public class JwtTokenProvider {

    private String secretKey;
    private long expirationMs;

    /**
     * Сгенерировать JWT-токен для пользователя.
     *
     * @param user пользователь
     * @return строка JWT-токена
     */
    public String generateToken(User user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Проверить валидность токена.
     *
     * @param token JWT-токен
     * @return {@code true}, если токен валиден и не просрочен
     */
    public boolean validateToken(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Извлечь идентификатор пользователя из токена.
     *
     * @param token JWT-токен
     * @return UUID пользователя
     */
    public UUID getUserIdFromToken(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Получить дату истечения токена.
     *
     * @param token JWT-токен
     * @return дата и время истечения
     */
    public LocalDateTime getExpirationDate(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
