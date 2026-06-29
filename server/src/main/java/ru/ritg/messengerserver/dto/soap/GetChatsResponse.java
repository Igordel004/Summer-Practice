package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * DTO: ответ со списком чатов (SOAP).
 *
 * <p>Содержит список собеседников пользователя.</p>
 */
@XmlRootElement(name = "GetChatsResponse", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class GetChatsResponse {

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private List<ContactDto> contacts;
}
