package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import ru.ritg.messengerserver.model.MessageStatus;

import java.util.UUID;

/**
 * DTO: элемент сообщения в истории (SOAP).
 */
@XmlRootElement(name = "MessageDto", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class MessageDto {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private UUID id;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private UUID senderId;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private UUID recipientId;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String payload;

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private MessageStatus status;

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private String createdAt;
}
