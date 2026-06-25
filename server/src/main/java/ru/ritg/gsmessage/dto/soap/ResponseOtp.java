package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.UUID;

/**
 * DTO: ответ на запрос OTP (SOAP).
 *
 * <p>Результат: флаг успеха и идентификатор запроса.</p>
 */
@XmlRootElement(name = "ResponseOtp", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ResponseOtp {

    @XmlElement(required = true)
    private boolean success;

    @XmlElement
    private UUID messageId;
}
