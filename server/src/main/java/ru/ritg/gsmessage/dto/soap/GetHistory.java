package ru.ritg.gsmessage.dto.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * DTO: запрос истории переписки (SOAP).
 *
 * <p>Входные данные: JWT-токен, смещение и лимит пагинации.</p>
 * <p>Ожидаемый результат: {@link HistoryResponse} — список сообщений и общее количество.</p>
 * <p>Возможные ошибки: {@code UnauthorizedException}, {@code InvalidOffsetException}.</p>
 */
@XmlRootElement(name = "GetHistory", namespace = "http://ritg.ru/gsmessage")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class GetHistory {

    @XmlElement(required = true)
    private String token;

    @XmlElement(required = true)
    private int offset;

    @XmlElement(required = true)
    private int limit;
}
