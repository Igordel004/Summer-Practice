package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.UUID;

/**
 * DTO: ответ на добавление контакта (SOAP).
 */
@XmlRootElement(name = "ContactResponse", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ContactResponse {

    @XmlElement(required = true)
    private boolean success;

    @XmlElement
    private UUID contactId;
}
