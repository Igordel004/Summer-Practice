package ru.ritg.messengerserver.dto.ws;

import lombok.Data;
import ru.ritg.messengerserver.model.MessageStatus;

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
    private String timestamp;
}
