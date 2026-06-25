package ru.ritg.gsmessage.exception;

/**
 * Исключение: неверный или просроченный OTP-код.
 *
 * <p>Выбрасывается при верификации, если код не найден, уже использован
 * или срок его действия истёк.</p>
 *
 * <p>SOAP Fault: {@code InvalidOtp}.</p>
 */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String phone) {
        super("Invalid or expired OTP for phone: " + phone);
    }
}
