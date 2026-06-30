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
import ru.ritg.messengerclient.model.AppState
import ru.ritg.messengerclient.network.SoapClient

/**
 * Модель отображения чата в списке.
 *
 * @property partnerId UUID собеседника (получен из {@code GetChats} или {@code FindUserByPhone})
 * @property phone номер телефона собеседника
 * @property nickname никнейм собеседника
 * @property lastMessage текст последнего сообщения
 * @property timestamp время последнего сообщения
 */
data class ChatConversation(
    val partnerId: String = "",
    val phone: String,
    val nickname: String = "",
    val lastMessage: String = "",
    val timestamp: String = ""
)

/**
 * ViewModel экрана списка чатов.
 *
 * Управляет списком активных бесед, загружает данные с сервера
 * и обновляет локальное состояние при отправке/получении сообщений.
 */
class ChatsViewModel(application: Application) : AndroidViewModel(application) {

    private val soapClient = SoapClient()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _unauthorized = MutableStateFlow(false)
    val unauthorized: StateFlow<Boolean> = _unauthorized.asStateFlow()

    /**
     * Загрузить список чатов с сервера.
     *
     * @param appState состояние приложения
     */
    fun loadChatsFromServer(appState: AppState) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(appState.serverHost, appState.serverPort)
                soapClient.getChats(appState.token)
            }
            result.onSuccess { xml ->
                val fault = soapClient.extractSoapFault(xml)
                if (fault.contains("Unauthorized", ignoreCase = true)) {
                    _unauthorized.value = true
                    return@onSuccess
                }
                val chats = parseChats(xml)
                _conversations.value = chats
            }
            result.onFailure { e ->
                android.util.Log.e("ChatsVM", "Load chats failed: ${e.message}")
                if (e.message?.contains("Unauthorized", ignoreCase = true) == true) {
                    _unauthorized.value = true
                }
            }
        }
    }

    private fun parseChats(xml: String): List<ChatConversation> {
        val contacts = mutableListOf<ChatConversation>()

        val contactIdPattern = "<[^>]*contactId>([^<]*)</[^>]*contactId>".toRegex()
        val phonePattern = "<[^>]*phone>([^<]*)</[^>]*phone>".toRegex()
        val nicknamePattern = "<[^>]*nickname>([^<]*)</[^>]*nickname>".toRegex()

        val contactIds = contactIdPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val phones = phonePattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val nicknames = nicknamePattern.findAll(xml).map { it.groupValues[1].trim() }.toList()

        for (i in contactIds.indices) {
            val phone = phones.getOrElse(i) { "" }
            if (phone.isNotBlank()) {
                contacts.add(
                    ChatConversation(
                        partnerId = contactIds[i],
                        phone = phone,
                        nickname = nicknames.getOrElse(i) { "" }
                    )
                )
            }
        }
        return contacts
    }

    /**
     * Добавить или обновить беседу в списке.
     *
     * @param phone номер телефона собеседника
     * @param nickname никнейм
     * @param lastMessage текст последнего сообщения
     * @param timestamp время последнего сообщения
     */
    fun addOrUpdateConversation(phone: String, nickname: String = "", lastMessage: String = "", timestamp: String = "") {
        val existing = _conversations.value.find { it.phone == phone }
        if (existing != null) {
            _conversations.value = _conversations.value.map {
                if (it.phone == phone) it.copy(
                    nickname = nickname.ifBlank { it.nickname },
                    lastMessage = lastMessage.ifBlank { it.lastMessage },
                    timestamp = timestamp.ifBlank { it.timestamp }
                ) else it
            }
        } else {
            _conversations.value = _conversations.value + ChatConversation(
                phone = phone, nickname = nickname, lastMessage = lastMessage, timestamp = timestamp
            )
        }
    }

    /**
     * Обновить последнее сообщение в беседе.
     *
     * @param phone номер телефона собеседника
     * @param message текст сообщения
     * @param timestamp время сообщения
     */
    fun updateLastMessage(phone: String, message: String, timestamp: String) {
        _conversations.value = _conversations.value.map {
            if (it.phone == phone) it.copy(lastMessage = message, timestamp = timestamp) else it
        }
    }

    /**
     * Удалить беседу из списка.
     *
     * @param phone номер телефона собеседника
     */
    fun removeConversation(phone: String) {
        _conversations.value = _conversations.value.filter { it.phone != phone }
    }

    /** Очистить список бесед. */
    fun clear() {
        _conversations.value = emptyList()
    }

    /** Сбросить флаг Unauthorized. */
    fun clearUnauthorized() {
        _unauthorized.value = false
    }
}
