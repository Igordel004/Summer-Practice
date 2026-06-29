package ru.ritg.messengerserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ritg.messengerserver.dto.soap.AuthResponse;
import ru.ritg.messengerserver.dto.soap.ResponseOtp;
import ru.ritg.messengerserver.exception.InvalidOtpException;
import ru.ritg.messengerserver.exception.InvalidPhoneException;
import ru.ritg.messengerserver.exception.UnauthorizedException;
import ru.ritg.messengerserver.model.Session;
import ru.ritg.messengerserver.model.User;
import ru.ritg.messengerserver.model.VerificationCode;
import ru.ritg.messengerserver.repository.SessionRepository;
import ru.ritg.messengerserver.repository.UserRepository;
import ru.ritg.messengerserver.repository.VerificationCodeRepository;
import ru.ritg.messengerserver.security.JwtTokenProvider;
import ru.ritg.messengerserver.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Сервис аутентификации и авторизации.
 *
 * <p>Управляет жизненным циклом OTP-кодов и JWT-сессий.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RATE_LIMIT_SECONDS = 60;

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final SmsGatewayService smsGatewayService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityUtils securityUtils;

    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       VerificationCodeRepository verificationCodeRepository,
                       SmsGatewayService smsGatewayService,
                       JwtTokenProvider jwtTokenProvider,
                       SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.smsGatewayService = smsGatewayService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityUtils = securityUtils;
    }

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
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new InvalidPhoneException(phone);
        }

        var recentCodes = verificationCodeRepository.findByPhone(phone);
        for (VerificationCode vc : recentCodes) {
            if (!vc.isUsed()
                    && LocalDateTime.now().isBefore(vc.getCreatedAt().plusSeconds(RATE_LIMIT_SECONDS))) {
                throw new InvalidPhoneException(phone);
            }
        }

        String code = securityUtils.generateOtp();
        log.info("Generated OTP for phone {}: {}", phone, code);

        VerificationCode verificationCode = VerificationCode.builder()
                .phone(phone)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .isUsed(false)
                .build();
        verificationCodeRepository.save(verificationCode);

        boolean smsSent = smsGatewayService.sendSms(phone, code);

        ResponseOtp response = new ResponseOtp();
        response.setSuccess(smsSent);
        response.setMessageId(verificationCode.getId());
        return response;
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
    @Transactional
    public AuthResponse verifyOtp(String phone, String code, String nickname) {
        VerificationCode verificationCode = verificationCodeRepository.findByPhoneAndCode(phone, code)
                .orElseThrow(() -> new InvalidOtpException(phone));

        if (verificationCode.isUsed() || verificationCode.isExpired()) {
            throw new InvalidOtpException(phone);
        }

        verificationCode.markUsed();
        verificationCodeRepository.save(verificationCode);

        User user;
        if (nickname != null && !nickname.isBlank()) {
            user = userRepository.findByPhone(phone)
                    .orElseGet(() -> {
                        User newUser = User.builder().phone(phone).nickname(nickname).build();
                        return userRepository.save(newUser);
                    });
        } else {
            user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new UnauthorizedException("Phone not registered: " + phone));
        }

        String token = jwtTokenProvider.generateToken(user);
        LocalDateTime expiresAt = jwtTokenProvider.getExpirationDate(token);

        Session session = Session.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        sessionRepository.save(session);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresAt(expiresAt);
        response.setNickname(user.getNickname());
        return response;
    }

    /**
     * Получить пользователя по JWT-токену.
     *
     * @param token JWT-токен
     * @return {@link Optional} с пользователем
     */
    public Optional<User> getUserByToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return Optional.empty();
        }
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        return userRepository.findById(userId);
    }

    /**
     * Обновить никнейм пользователя.
     *
     * @param user     пользователь
     * @param nickname новый никнейм
     * @return обновлённый никнейм
     */
    @Transactional
    public String updateNickname(User user, String nickname) {
        user.setNickname(nickname);
        userRepository.save(user);
        log.info("Nickname updated for user {}: {}", user.getId(), nickname);
        return nickname;
    }
}
