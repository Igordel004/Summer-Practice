package ru.ritg.messengerserver.dto.ws;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: входящее сообщение по WebSocket (JSON).
 *
 * <p>Входные данные: идентификаторы отправителя/получателя, текст, опциональная ссылка на reply.</p>
 * <p>Ожидаемый результат: доставка через {@link WsDelivery} или буферизация (PENDING).</p>
 * <p>Возможные ошибки: 1008 Policy Violation (невалидный JSON), Unauthorized.</p>
 */
@Data
public class WsMessage {

    private String type;
    private UUID senderId;
    private UUID recipientId;
    private String payload;
    private UUID replyToId;
    private LocalDateTime timestamp;
}
