package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос списка контактов (SOAP).
 *
 * <p>Входные данные: JWT-токен.</p>
 * <p>Ожидаемый результат: {@link GetContactsResponse} — адресная книга.</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}.</p>
 */
@XmlRootElement(name = "GetContacts", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class GetContacts {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;
}
