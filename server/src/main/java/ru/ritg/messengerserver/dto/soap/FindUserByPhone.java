package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос поиска пользователя по номеру телефона (SOAP).
 *
 * <p>Входные данные: JWT-токен и номер телефона.</p>
 * <p>Ожидаемый результат: {@link FindUserByPhoneResponse} — найден ли пользователь, его UUID, телефон и никнейм.</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}.</p>
 */
@XmlRootElement(name = "FindUserByPhone", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class FindUserByPhone {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String phone;
}
