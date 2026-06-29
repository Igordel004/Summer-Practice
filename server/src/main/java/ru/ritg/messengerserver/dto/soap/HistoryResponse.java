package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * DTO: ответ с историей сообщений (SOAP).
 */
@XmlRootElement(name = "HistoryResponse", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class HistoryResponse {

    @XmlElement(namespace = SoapConstants.NAMESPACE)
    private List<MessageDto> messages;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private int total;
}
