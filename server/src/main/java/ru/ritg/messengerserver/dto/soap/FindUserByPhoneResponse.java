package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.UUID;

/**
 * DTO: ответ на поиск пользователя по номеру телефона (SOAP).
 *
 * <p>Содержит флаг {@code found}, и при нахождении — UUID, телефон и никнейм пользователя.</p>
 * <p>Используется клиентом для получения UUID собеседника перед открытием чата
 * с пользователем, который не добавлен в контакты.</p>
 */
@XmlRootElement(name = "FindUserByPhoneResponse", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class FindUserByPhoneResponse {

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private boolean found;

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private UUID userId;

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private String phone;

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private String nickname;
}
