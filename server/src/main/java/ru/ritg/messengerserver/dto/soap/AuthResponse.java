package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Data;
import ru.ritg.messengerserver.config.LocalDateTimeAdapter;

import java.time.LocalDateTime;

/**
 * DTO: ответ успешной авторизации (SOAP).
 *
 * <p>Результат: JWT-токен и дата истечения.</p>
 */
@XmlRootElement(name = "AuthResponse", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class AuthResponse {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime expiresAt;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = false)
    private String nickname;
}
