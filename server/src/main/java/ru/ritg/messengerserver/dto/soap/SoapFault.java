package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: SOAP Fault с кодом ошибки и описанием.
 *
 * <p>Используется при ошибках SOAP-запросов.</p>
 */
@XmlRootElement(name = "SoapFault", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class SoapFault {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String code;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String message;
}
