package ru.ritg.messengerclient.model

import java.util.UUID

/**
 * Модель контакта пользователя.
 *
 * @property phone номер телефона в формате E.164
 * @property nickname отображаемое имя (пустая строка, если не задано)
 * @property userId идентификатор пользователя на сервере
 */
data class Contact(
    val phone: String,
    val nickname: String = "",
    val userId: UUID? = null
)

/**
 * Модель сообщения в чате.
 *
 * @property id идентификатор сообщения на сервере
 * @property senderId идентификатор отправителя
 * @property recipientId идентификатор получателя
 * @property payload текст сообщения
 * @property status статус доставки (PENDING / DELIVERED / READ)
 * @property isOwn принадлежит ли сообщение текущему пользователю
 * @property timestamp отформатированная временная метка
 */
data class ChatMessage(
    val id: UUID? = null,
    val senderId: UUID? = null,
    val recipientId: UUID? = null,
    val payload: String,
    val status: String = "PENDING",
    val isOwn: Boolean = false,
    val timestamp: String = ""
)

/**
 * Глобальное состояние приложения.
 *
 * Хранит JWT-токен, данные пользователя и параметры подключения к серверу.
 *
 * @property token JWT-токен сессии
 * @property userId идентификатор текущего пользователя
 * @property phone номер телефона текущего пользователя
 * @property nickname никнейм текущего пользователя
 * @property serverHost хост сервера (по умолчанию 10.0.2.2 — эмулятор)
 * @property serverPort порт SOAP (по умолчанию 8080)
 * @property wsPort порт WebSocket (по умолчанию 8081)
 */
data class AppState(
    val token: String = "",
    val userId: UUID? = null,
    val phone: String = "",
    val nickname: String = "",
    val serverHost: String = "10.0.2.2",
    val serverPort: Int = 8080,
    val wsPort: Int = 8081
)


