package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос на добавление контакта (SOAP).
 *
 * <p>Входные данные: JWT-токен и номер телефона контакта.</p>
 * <p>Ожидаемый результат: {@link ContactResponse} — подтверждение добавления.</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}, {@code ContactNotFoundException}.</p>
 */
@XmlRootElement(name = "AddContact", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class AddContact {

    @XmlElement(required = true)
    private String token;

    @XmlElement(required = true)
    private String contactPhone;
}
