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
 * <p>Входные данные: JWT-токен, UUID собеседника ({@code partnerId}),
 * смещение и лимит пагинации.</p>
 * <p>Ожидаемый результат: {@link HistoryResponse} — список сообщений и общее количество.
 * Сообщения возвращаются в хронологическом порядке (от старых к новым).</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}, {@code MessageNotFoundException}.</p>
 *
 * <p>Параметр {@code partnerId} — UUID другого участника переписки.
 * Серверу неважно, добавлен ли он в контакты — достаточно наличия переписки в БД.</p>
 */
@XmlRootElement(name = "GetHistory", namespace = SoapConstants.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class GetHistory {

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private String token;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private UUID partnerId;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private int offset;

    @XmlElement(namespace = SoapConstants.NAMESPACE, required = true)
    private int limit;
}
