package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос на смену никнейма (SOAP).
 *
 * <p>Входные данные: токен и новый никнейм.</p>
 * <p>Ожидаемый результат: {@link UpdateNicknameResponse} — подтверждение.</p>
 */
@XmlRootElement(name = "UpdateNickname", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class UpdateNickname {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String nickname;
}
