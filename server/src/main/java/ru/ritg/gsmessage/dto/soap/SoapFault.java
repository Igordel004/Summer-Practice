package ru.ritg.gsmessage.dto.soap;

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
@XmlRootElement(name = "SoapFault", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class SoapFault {

    @XmlElement(required = true)
    private String code;

    @XmlElement(required = true)
    private String message;
}
