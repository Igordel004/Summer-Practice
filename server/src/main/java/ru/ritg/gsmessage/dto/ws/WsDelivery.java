package ru.ritg.gsmessage.dto.ws;

import lombok.Data;
import ru.ritg.gsmessage.model.MessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: доставка сообщения получателю через WebSocket (JSON).
 *
 * <p>Содержит полный payload и текущий статус доставки.</p>
 */
@Data
public class WsDelivery {

    private String type;
    private UUID messageId;
    private UUID senderId;
    private UUID recipientId;
    private String payload;
    private MessageStatus status;
    private LocalDateTime timestamp;
}
