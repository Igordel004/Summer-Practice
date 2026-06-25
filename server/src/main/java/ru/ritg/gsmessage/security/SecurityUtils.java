package ru.ritg.gsmessage.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Утилиты безопасности.
 *
 * <p>Содержит метод для генерации OTP-кодов.
 * Использует {@link SecureRandom} для криптографически стойкой генерации.</p>
 */
@Component
public class SecurityUtils {

    private static final int OTP_LENGTH = 4;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Сгенерировать 4-значный числовой OTP-код.
     *
     * <p>Использует {@link SecureRandom} для равномерного распределения.
     * Диапазон: 0000–9999.</p>
     *
     * @return строка из 4 цифр (например, "0427")
     */
    public String generateOtp() {
        int code = SECURE_RANDOM.nextInt(10000);
        return String.format("%04d", code);
    }
}
