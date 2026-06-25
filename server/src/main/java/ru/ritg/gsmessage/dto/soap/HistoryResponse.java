package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * DTO: ответ с историей сообщений (SOAP).
 */
@XmlRootElement(name = "HistoryResponse", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class HistoryResponse {

    @XmlElement
    private List<MessageDto> messages;

    @XmlElement(required = true)
    private int total;
}
