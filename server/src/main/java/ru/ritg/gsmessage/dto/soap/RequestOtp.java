package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос на генерацию OTP-кода (SOAP).
 *
 * <p>Входные данные: номер телефона в формате E.164.</p>
 * <p>Ожидаемый результат: {@link ResponseOtp} — подтверждение отправки.</p>
 * <p>Возможные ошибки: {@code InvalidPhoneException}, {@code TooManyRequestsException}.</p>
 */
@XmlRootElement(name = "RequestOtp", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class RequestOtp {

    @XmlElement(required = true)
    private String phone;
}
