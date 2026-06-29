package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос на удаление контакта (SOAP).
 *
 * <p>Входные данные: JWT-токен и номер телефона контакта.</p>
 * <p>Ожидаемый результат: {@link ContactResponse} — подтверждение удаления.</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}, ContactNotFound.</p>
 */
@XmlRootElement(name = "DeleteContact", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class DeleteContact {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String contactPhone;
}
