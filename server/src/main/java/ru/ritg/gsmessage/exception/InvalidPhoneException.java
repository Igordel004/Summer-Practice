package ru.ritg.gsmessage.exception;

/**
 * Исключение: неверный формат номера телефона.
 *
 * <p>Выбрасывается при запросе OTP, если номер не соответствует
 * формату E.164 (+7XXXXXXXXXX, 11–12 цифр).</p>
 *
 * <p>SOAP Fault: {@code InvalidPhone}.</p>
 */
public class InvalidPhoneException extends RuntimeException {

    public InvalidPhoneException(String phone) {
        super("Invalid phone format: " + phone);
    }
}
