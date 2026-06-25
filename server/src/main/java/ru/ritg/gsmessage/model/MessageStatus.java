package ru.ritg.gsmessage.model;

/**
 * Перечисление статусов доставки сообщения.
 *
 * <p>Статусы отражают жизненный цикл сообщения в системе:</p>
 * <ul>
 *   <li>{@code PENDING} — сообщение создано, но ещё не доставлено получателю</li>
 *   <li>{@code DELIVERED} — сообщение доставлено на устройство получателя</li>
 *   <li>{@code READ} — сообщение прочитано получателем</li>
 * </ul>
 */
public enum MessageStatus {
    PENDING,
    DELIVERED,
    READ
}
