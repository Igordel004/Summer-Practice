package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.UUID;

/**
 * DTO: ответ на добавление/удаление контакта (SOAP).
 *
 * <p>Содержит флаг успеха, идентификатор контакта (при добавлении)
 * и описание результата операции.</p>
 */
@XmlRootElement(name = "ContactResponse", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ContactResponse {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private boolean success;

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private UUID contactId;

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private String message;
}
