package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO: ответ успешной авторизации (SOAP).
 *
 * <p>Результат: JWT-токен и дата истечения.</p>
 */
@XmlRootElement(name = "AuthResponse", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class AuthResponse {

    @XmlElement(required = true)
    private String token;

    @XmlElement(required = true)
    private LocalDateTime expiresAt;
}
