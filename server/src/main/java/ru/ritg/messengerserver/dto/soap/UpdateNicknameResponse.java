package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: ответ смены никнейма (SOAP).
 *
 * <p>Результат: статус операции и текущий никнейм.</p>
 */
@XmlRootElement(name = "UpdateNicknameResponse", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class UpdateNicknameResponse {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private boolean success;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = false)
    private String nickname;
}
