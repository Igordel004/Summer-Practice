package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос на верификацию OTP-кода (SOAP).
 *
 * <p>Входные данные: номер телефона и 4-значный код.</p>
 * <p>Ожидаемый результат: {@link AuthResponse} — JWT-токен и срок действия.</p>
 * <p>Возможные ошибки: {@code InvalidOtpException}, {@code OtpExpiredException}.</p>
 */
@XmlRootElement(name = "VerifyOtp", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class VerifyOtp {

    @XmlElement(required = true)
    private String phone;

    @XmlElement(required = true)
    private String code;
}
