package ru.ritg.gsmessage.service;

import org.springframework.stereotype.Service;
import ru.ritg.gsmessage.dto.soap.AuthResponse;
import ru.ritg.gsmessage.dto.soap.ResponseOtp;
import ru.ritg.gsmessage.model.User;

import java.util.Optional;

/**
 * Сервис аутентификации и авторизации.
 *
 * <p>Управляет жизненным циклом OTP-кодов и JWT-сессий.</p>
 */
@Service
public class AuthService {

    /**
     * Сгенерировать и отправить OTP-код.
     *
     * <p>Входные данные: номер телефона в формате E.164 (+7XXXXXXXXXX).</p>
     * <p>Параметры:</p>
     * <ul>
     *   <li>{@code phone} — обязательный, 11–12 цифр, регулярное выражение ^\+7\d{10}$</li>
     * </ul>
     * <p>Ожидаемый результат: {@link ResponseOtp} с {@code success=true} и {@code messageId}.</p>
     * <p>Возможные ошибки:</p>
     * <ul>
     *   <li>{@code InvalidPhoneException} — номер не соответствует формату</li>
     *   <li>{@code TooManyRequestsException} — повторный запрос раньше 60 секунд</li>
     * </ul>
     *
     * @param phone номер телефона
     * @return ответ с подтверждением отправки
     */
    public ResponseOtp generateOtp(String phone) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Проверить OTP-код и создать сессию (JWT).
     *
     * <p>Входные данные: номер телефона и 4-значный код.</p>
     * <p>Параметры:</p>
     * <ul>
     *   <li>{@code phone} — обязательный</li>
     *   <li>{@code code} — обязательный, ровно 4 цифры</li>
     * </ul>
     * <p>Ожидаемый результат: {@link AuthResponse} с JWT-токеном и сроком действия.</p>
     * <p>Возможные ошибки:</p>
     * <ul>
     *   <li>{@code InvalidOtpException} — код не найден или неверен</li>
     *   <li>{@code OtpExpiredException} — срок действия кода истёк</li>
     * </ul>
     *
     * @param phone номер телефона
     * @param code  OTP-код
     * @return ответ с JWT-токеном
     */
    public AuthResponse verifyOtp(String phone, String code) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Проверить валидность JWT-токена.
     *
     * @param token JWT-токен
     * @return {@code true}, если токен валиден и не просрочен
     */
    public boolean isValidToken(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Получить пользователя по JWT-токену.
     *
     * @param token JWT-токен
     * @return {@link Optional} с пользователем
     */
    public Optional<User> getUserByToken(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Очистить просроченные OTP-коды (вызов из {@link ScheduledTasksService}).
     */
    public void cleanupExpiredCodes() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
