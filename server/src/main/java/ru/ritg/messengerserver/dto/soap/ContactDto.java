package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.UUID;

/**
 * DTO: элемент контакта в списке (SOAP).
 *
 * <p>Используется в ответах GetContacts и GetChats.</p>
 */
@XmlRootElement(name = "ContactDto", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ContactDto {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private UUID contactId;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String phone;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String nickname;
}
