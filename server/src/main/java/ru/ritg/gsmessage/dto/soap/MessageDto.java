package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import ru.ritg.gsmessage.model.MessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: элемент сообщения в истории (SOAP).
 */
@XmlRootElement(name = "MessageDto", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class MessageDto {

    @XmlElement(required = true)
    private UUID id;

    @XmlElement(required = true)
    private UUID senderId;

    @XmlElement(required = true)
    private UUID recipientId;

    @XmlElement(required = true)
    private String payload;

    @XmlElement
    private MessageStatus status;

    @XmlElement
    private LocalDateTime createdAt;
}
