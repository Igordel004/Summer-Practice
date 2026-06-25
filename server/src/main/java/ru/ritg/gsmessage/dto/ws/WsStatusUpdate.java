package ru.ritg.gsmessage.dto.ws;

import lombok.Data;
import ru.ritg.gsmessage.model.MessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: обновление статуса сообщения по WebSocket (JSON).
 *
 * <p>Входные данные: идентификатор сообщения и новый статус (READ).</p>
 * <p>Ожидаемый результат: ack-подтверждение и обновление в БД.</p>
 * <p>Возможные ошибки: {@code MessageNotFoundException}, Unauthorized.</p>
 */
@Data
public class WsStatusUpdate {

    private String type;
    private UUID messageId;
    private MessageStatus status;
    private LocalDateTime timestamp;
}
