package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос списка чатов (SOAP).
 *
 * <p>Входные данные: JWT-токен.</p>
 * <p>Ожидаемый результат: {@link GetChatsResponse} — список собеседников.</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}.</p>
 */
@XmlRootElement(name = "GetChats", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class GetChats {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;
}
