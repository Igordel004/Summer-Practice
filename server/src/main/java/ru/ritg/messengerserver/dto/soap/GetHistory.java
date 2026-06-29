package ru.ritg.messengerserver.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.UUID;

/**
 * DTO: запрос истории переписки (SOAP).
 *
 * <p>Входные данные: JWT-токен, смещение и лимит пагинации.</p>
 * <p>Ожидаемый результат: {@link HistoryResponse} — список сообщений и общее количество.</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}, {@code InvalidOffsetException}.</p>
 */
@XmlRootElement(name = "GetHistory", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class GetHistory {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private UUID contactId;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private int offset;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private int limit;
}
