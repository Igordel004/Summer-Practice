package ru.ritg.messengerclient.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.ritg.messengerclient.model.AppState
import ru.ritg.messengerclient.model.ChatMessage
import ru.ritg.messengerclient.network.SoapClient
import ru.ritg.messengerclient.network.WsClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel экрана чата.
 *
 * Управляет WebSocket-соединением, отправкой/приёмом сообщений,
 * загрузкой истории, обновлением статусов (DELIVERED / READ).
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val wsClient = WsClient()
    private val soapClient = SoapClient()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    private var myUserId: UUID? = null
    private var recipientPhone: String = ""
    private var lastAppState: AppState? = null
    private var pendingMessage: Triple<UUID, UUID, String>? = null
    private var currentContactId: UUID? = null
    private var totalMessages: Int = 0
    private var loadedCount: Int = 0
    private val PAGE_SIZE = 20

    // Pending acks keyed by payload, to handle race condition where ack arrives before local message is added
    private val pendingAcks = ConcurrentHashMap<String, PendingAck>()

    private data class PendingAck(
        val messageId: UUID?,
        val status: String,
        val serverTimestamp: String?
    )

    companion object {
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        private val SERVER_TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    /**
     * Установить глобальное WebSocket-соединение для текущего пользователя.
     *
     * @param appState состояние приложения (токен, хост, порт)
     */
    fun connectGlobal(appState: AppState) {
        myUserId = appState.userId
        lastAppState = appState

        wsClient.disconnect()

        wsClient.listener = object : WsClient.WsListener {
            override fun onConnected() {
                _connected.value = true
                _statusMessage.value = "Подключено"
                sendPendingMessage()
            }

            override fun onMessage(json: String) {
                handleIncomingMessage(json)
            }

            override fun onDisconnected(code: Int, reason: String) {
                _connected.value = false
                _statusMessage.value = "Отключено: $reason"
            }

            override fun onError(error: String) {
                _statusMessage.value = "Ошибка: $error"
            }
        }
        wsClient.connect(appState.serverHost, appState.wsPort, appState.token)
    }

    /** Установить номер телефона собеседника. */
    fun setRecipient(phone: String) {
        recipientPhone = phone
    }

    private fun ensureConnected() {
        if (!wsClient.isConnected()) {
            val appState = lastAppState ?: return
            wsClient.connect(appState.serverHost, appState.wsPort, appState.token)
        }
    }

    /**
     * Отправить сообщение через WebSocket.
     *
     * @param senderId UUID отправителя
     * @param recipientId UUID получателя
     * @param payload текст сообщения
     */
    fun sendMessage(senderId: UUID, recipientId: UUID, payload: String) {
        ensureConnected()
        val msg = JSONObject().apply {
            put("type", "message")
            put("senderId", senderId.toString())
            put("recipientId", recipientId.toString())
            put("payload", payload)
        }
        wsClient.sendMessage(msg.toString())

        val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
        val chatMsg = ChatMessage(
            payload = payload,
            isOwn = true,
            status = "PENDING",
            timestamp = timestamp,
            senderId = senderId,
            recipientId = recipientId
        )
        viewModelScope.launch {
            // Check if an ack arrived before this message was added to the list
            val ack = pendingAcks.remove(payload)
            val finalMsg = if (ack != null) {
                chatMsg.copy(
                    id = ack.messageId ?: chatMsg.id,
                    status = ack.status,
                    timestamp = ack.serverTimestamp ?: chatMsg.timestamp
                )
            } else {
                chatMsg
            }
            _messages.value = _messages.value + finalMsg
        }
    }

    private fun handleIncomingMessage(json: String) {
        try {
            val obj = JSONObject(json)
            val type = obj.optString("type", "")

            when (type) {
                "delivery" -> {
                    val payload = obj.optString("payload", "")
                    val messageId = obj.optString("messageId", "")
                    val senderId = obj.optString("senderId", "")
                    val serverTimestamp = obj.optString("timestamp", "")
                    val timestamp = parseServerTimestamp(serverTimestamp)

                    val chatMsg = ChatMessage(
                        id = if (messageId.isNotEmpty()) UUID.fromString(messageId) else null,
                        senderId = if (senderId.isNotEmpty()) UUID.fromString(senderId) else null,
                        payload = payload,
                        isOwn = false,
                        status = "DELIVERED",
                        timestamp = timestamp
                    )
                    viewModelScope.launch {
                        _messages.value = _messages.value + chatMsg
                    }
                }
                "ack" -> {
                    val messageId = obj.optString("messageId", "")
                    val status = obj.optString("status", "DELIVERED")
                    val payloadStr = obj.optString("payload", "")
                    val serverTimestamp = obj.optString("timestamp", "")
                    val parsedTimestamp = parseServerTimestamp(serverTimestamp)

                    val ack = PendingAck(
                        messageId = if (messageId.isNotEmpty()) try { UUID.fromString(messageId) } catch (_: Exception) { null } else null,
                        status = status,
                        serverTimestamp = if (parsedTimestamp.isNotEmpty()) parsedTimestamp else null
                    )

                    viewModelScope.launch {
                        // Try to apply ack to existing messages
                        val updated = _messages.value.map { msg ->
                            if (msg.id?.toString() == messageId || (msg.isOwn && msg.status == "PENDING" && msg.payload == payloadStr)) {
                                val newId = ack.messageId ?: msg.id
                                val newTimestamp = ack.serverTimestamp ?: msg.timestamp
                                msg.copy(id = newId, status = status, timestamp = newTimestamp)
                            } else msg
                        }
                        val matched = updated.any { it.id?.toString() == messageId || (it.isOwn && it.payload == payloadStr && it.status != "PENDING") }
                        if (matched) {
                            _messages.value = updated
                        } else {
                            // Race condition: ack arrived before local message was added — store for later
                            pendingAcks[payloadStr] = ack
                            _messages.value = updated
                        }
                    }
                }
                "status_ack" -> {
                    val messageId = obj.optString("messageId", "")
                    val status = obj.optString("status", "")
                    if (messageId.isNotEmpty() && status.isNotEmpty()) {
                        viewModelScope.launch {
                            val updated = _messages.value.map { msg ->
                                if (msg.id?.toString() == messageId) {
                                    msg.copy(status = status)
                                } else msg
                            }
                            _messages.value = updated
                        }
                    }
                }
                "edit_ack" -> {
                    val messageId = obj.optString("messageId", "")
                    val newPayload = obj.optString("newPayload", "")
                    if (messageId.isNotEmpty()) {
                        viewModelScope.launch {
                            val updated = _messages.value.map { msg ->
                                if (msg.id?.toString() == messageId) {
                                    msg.copy(payload = newPayload)
                                } else msg
                            }
                            _messages.value = updated
                        }
                    }
                }
                "edit_notify" -> {
                    val messageId = obj.optString("messageId", "")
                    val newPayload = obj.optString("newPayload", "")
                    if (messageId.isNotEmpty()) {
                        viewModelScope.launch {
                            val updated = _messages.value.map { msg ->
                                if (msg.id?.toString() == messageId) {
                                    msg.copy(payload = newPayload)
                                } else msg
                            }
                            _messages.value = updated
                        }
                    }
                }
                "delete_ack" -> {
                    val messageId = obj.optString("messageId", "")
                    if (messageId.isNotEmpty()) {
                        viewModelScope.launch {
                            _messages.value = _messages.value.filter { it.id?.toString() != messageId }
                        }
                    }
                }
                "delete_notify" -> {
                    val messageId = obj.optString("messageId", "")
                    if (messageId.isNotEmpty()) {
                        viewModelScope.launch {
                            _messages.value = _messages.value.filter { it.id?.toString() != messageId }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _statusMessage.value = "Ошибка парсинга: ${e.message}"
            }
        }
    }

    /**
     * Отправить обновление статуса сообщения (READ).
     *
     * @param messageId UUID сообщения
     */
    fun updateStatus(messageId: UUID) {
        val msg = JSONObject().apply {
            put("type", "status")
            put("messageId", messageId.toString())
            put("status", "READ")
        }
        wsClient.sendMessage(msg.toString())
    }

    /** Пометить все входящие сообщения как прочитанные (READ). */
    fun markIncomingAsRead() {
        viewModelScope.launch {
            val toMark = _messages.value.filter { !it.isOwn && it.status != "READ" && it.id != null }
            val updated = _messages.value.map { msg ->
                if (!msg.isOwn && msg.status != "READ") msg.copy(status = "READ") else msg
            }
            _messages.value = updated
            for (msg in toMark) {
                val json = JSONObject().apply {
                    put("type", "status")
                    put("messageId", msg.id.toString())
                    put("status", "READ")
                }
                wsClient.sendMessage(json.toString())
            }
        }
    }

    /**
     * Загрузить историю переписки с сервера (последние 20 сообщений).
     *
     * @param appState состояние приложения
     * @param recipientId UUID собеседника
     * @param offset смещение
     * @param limit максимальное количество сообщений
     * @param onComplete callback со списком сообщений
     */
    fun loadHistory(appState: AppState, recipientId: java.util.UUID, offset: Int, limit: Int, onComplete: (List<ChatMessage>) -> Unit) {
        currentContactId = recipientId
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(appState.serverHost, appState.serverPort)
                soapClient.getHistory(appState.token, recipientId, offset, limit)
            }
            result.onSuccess { xml ->
                val (history, total) = parseHistoryWithTotal(xml, appState)
                totalMessages = total
                loadedCount = history.size
                _hasMoreMessages.value = loadedCount < totalMessages
                onComplete(history)
            }
            result.onFailure { e ->
                _statusMessage.value = "Ошибка загрузки: ${e.message}"
                onComplete(emptyList())
            }
        }
    }

    /**
     * Загрузить следующую порцию сообщений (при прокрутке вверх).
     */
    fun loadMoreMessages(appState: AppState) {
        if (_isLoadingMore.value || !_hasMoreMessages.value) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(appState.serverHost, appState.serverPort)
                soapClient.getHistory(appState.token, currentContactId!!, loadedCount, PAGE_SIZE)
            }
            result.onSuccess { xml ->
                val (pagedMessages, total) = parseHistoryWithTotal(xml, appState)
                totalMessages = total
                loadedCount += pagedMessages.size
                _hasMoreMessages.value = loadedCount < totalMessages
                _messages.value = pagedMessages + _messages.value
                _isLoadingMore.value = false
            }
            result.onFailure { e ->
                _isLoadingMore.value = false
            }
        }
    }

    private fun parseHistoryWithTotal(xml: String, appState: AppState): Pair<List<ChatMessage>, Int> {
        val messages = parseHistory(xml, appState)
        val totalPattern = "<[^>]*total>([^<]*)</[^>]*total>".toRegex()
        val totalMatch = totalPattern.find(xml)
        val total = totalMatch?.groupValues?.get(1)?.trim()?.toIntOrNull() ?: messages.size
        return Pair(messages, total)
    }

    private fun parseHistory(xml: String, appState: AppState): List<ChatMessage> {
        android.util.Log.d("ChatVM", "History XML: $xml")
        val messages = mutableListOf<ChatMessage>()

        val idPattern = "<[^>]*id>([^<]*)</[^>]*id>".toRegex()
        val senderIdPattern = "<[^>]*senderId>([^<]*)</[^>]*senderId>".toRegex()
        val recipientIdPattern = "<[^>]*recipientId>([^<]*)</[^>]*recipientId>".toRegex()
        val payloadPattern = "<[^>]*payload>([^<]*)</[^>]*payload>".toRegex()
        val statusPattern = "<[^>]*status>([^<]*)</[^>]*status>".toRegex()
        val createdAtPattern = "<[^>]*createdAt>([^<]*)</[^>]*createdAt>".toRegex()

        val ids = idPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val senderIds = senderIdPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val recipientIds = recipientIdPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val payloads = payloadPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val statuses = statusPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val createdAts = createdAtPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()

        for (i in ids.indices) {
            val senderId = senderIds.getOrElse(i) { "" }
            val recipientId = recipientIds.getOrElse(i) { "" }
            val isOwn = senderId == appState.userId?.toString()
            val rawTimestamp = createdAts.getOrElse(i) { "" }
            val timestamp = parseServerTimestamp(rawTimestamp)
            messages.add(
                ChatMessage(
                    id = try { UUID.fromString(ids[i]) } catch (_: Exception) { null },
                    senderId = try { UUID.fromString(senderId) } catch (_: Exception) { null },
                    recipientId = try { UUID.fromString(recipientId) } catch (_: Exception) { null },
                    payload = payloads.getOrElse(i) { "" },
                    isOwn = isOwn,
                    status = statuses.getOrElse(i) { "" },
                    timestamp = timestamp
                )
            )
        }
        android.util.Log.d("ChatVM", "Parsed ${messages.size} messages")
        return messages
    }

    private fun parseServerTimestamp(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            val dt = LocalDateTime.parse(raw, SERVER_TIMESTAMP_FORMAT)
            dt.format(TIMESTAMP_FORMAT)
        } catch (_: Exception) {
            try {
                val cleaned = raw.replace(Regex("\\.\\d+$"), "")
                val dt = LocalDateTime.parse(cleaned, SERVER_TIMESTAMP_FORMAT)
                dt.format(TIMESTAMP_FORMAT)
            } catch (_: Exception) {
                ""
            }
        }
    }

    /** Закрыть WebSocket-соединение. */
    fun disconnect() {
        wsClient.disconnect()
        _connected.value = false
    }

    /**
     * Отредактировать сообщение через WebSocket.
     *
     * @param messageId UUID сообщения
     * @param newPayload новый текст
     */
    fun editMessage(messageId: UUID, newPayload: String) {
        ensureConnected()
        val msg = JSONObject().apply {
            put("type", "edit")
            put("messageId", messageId.toString())
            put("newPayload", newPayload)
        }
        wsClient.sendMessage(msg.toString())
    }

    /**
     * Удалить сообщение через WebSocket.
     *
     * @param messageId UUID сообщения
     */
    fun deleteMessage(messageId: UUID) {
        ensureConnected()
        val msg = JSONObject().apply {
            put("type", "delete")
            put("messageId", messageId.toString())
        }
        wsClient.sendMessage(msg.toString())
    }

    /**
     * Сохранить сообщение для отправки после установления соединения.
     *
     * @param senderId UUID отправителя
     * @param recipientId UUID получателя
     * @param payload текст сообщения
     */
    fun setPendingMessage(senderId: UUID, recipientId: UUID, payload: String) {
        pendingMessage = Triple(senderId, recipientId, payload)
    }

    private fun sendPendingMessage() {
        pendingMessage?.let { (senderId, recipientId, payload) ->
            pendingMessage = null
            sendMessage(senderId, recipientId, payload)
        }
    }

    /** Очистить статусное сообщение. */
    fun clearStatus() {
        _statusMessage.value = null
    }

    /**
     * Установить список сообщений (например, из загруженной истории).
     *
     * @param messages список сообщений
     */
    fun setMessages(messages: List<ChatMessage>) {
        _messages.value = messages
    }

    /** Очистить список сообщений. */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /** Полностью очистить состояние ViewModel и закрыть соединение. */
    fun clear() {
        _messages.value = emptyList()
        _connected.value = false
        _statusMessage.value = null
        _isLoadingMore.value = false
        _hasMoreMessages.value = true
        myUserId = null
        recipientPhone = ""
        lastAppState = null
        currentContactId = null
        totalMessages = 0
        loadedCount = 0
        pendingMessage = null
        pendingAcks.clear()
        wsClient.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }
}
