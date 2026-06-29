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
import ru.ritg.messengerclient.model.Contact
import ru.ritg.messengerclient.network.SoapClient

/**
 * ViewModel экрана контактов.
 *
 * Управляет загрузкой, добавлением и удалением контактов через SOAP.
 */
class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val soapClient = SoapClient()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _unauthorized = MutableStateFlow(false)
    val unauthorized: StateFlow<Boolean> = _unauthorized.asStateFlow()

    /**
     * Загрузить контакты с сервера, если пользователь авторизован.
     *
     * @param appState состояние приложения
     */
    fun loadFromServerIfLoggedIn(appState: AppState) {
        if (appState.token.isNotEmpty()) {
            loadContactsFromServer(appState)
        }
    }

    /**
     * Принудительно обновить список контактов с сервера.
     *
     * @param appState состояние приложения
     */
    fun refresh(appState: AppState) {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadContactsFromServer(appState)
            _isRefreshing.value = false
        }
    }

    /**
     * Добавить контакт по номеру телефона.
     *
     * @param appState состояние приложения
     * @param contactPhone номер телефона контакта
     */
    fun addContact(appState: AppState, contactPhone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(appState.serverHost, appState.serverPort)
                soapClient.addContact(appState.token, contactPhone)
            }
            result.onSuccess { xml ->
                val message = soapClient.extractTag(xml, "message")
                val success = soapClient.extractTag(xml, "success")
                if (success == "true") {
                    val existing = _contacts.value.find { it.phone == contactPhone }
                    if (existing == null) {
                        _contacts.value = _contacts.value + Contact(phone = contactPhone)
                    }
                    loadContactsFromServer(appState)
                }
                _message.value = message.ifBlank { if (success == "true") "Контакт добавлен" else "Ошибка" }
            }
            result.onFailure { e ->
                _message.value = "Ошибка сети: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    /**
     * Удалить контакт.
     *
     * @param appState состояние приложения
     * @param contact контакт для удаления
     */
    fun removeContact(appState: AppState, contact: Contact) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(appState.serverHost, appState.serverPort)
                soapClient.deleteContact(appState.token, contact.phone)
            }
            result.onSuccess { xml ->
                val success = soapClient.extractTag(xml, "success")
                if (success == "true") {
                    _contacts.value = _contacts.value.filter { it.phone != contact.phone }
                    _message.value = "Контакт удалён"
                } else {
                    val msg = soapClient.extractTag(xml, "message")
                    _message.value = msg.ifBlank { "Ошибка удаления" }
                }
            }
            result.onFailure { e ->
                _message.value = "Ошибка сети: ${e.message}"
            }
        }
    }

    /** Очистить сообщение об ошибке/успехе. */
    fun clearMessage() {
        _message.value = null
    }

    /** Сбросить флаг Unauthorized. */
    fun clearUnauthorized() {
        _unauthorized.value = false
    }

    private fun loadContactsFromServer(appState: AppState) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(appState.serverHost, appState.serverPort)
                soapClient.getContacts(appState.token)
            }
            result.onSuccess { xml ->
                val fault = soapClient.extractSoapFault(xml)
                if (fault.contains("Unauthorized", ignoreCase = true)) {
                    _unauthorized.value = true
                    return@onSuccess
                }
                android.util.Log.d("ContactsVM", "XML: $xml")
                val contactsList = parseContacts(xml)
                android.util.Log.d("ContactsVM", "Parsed ${contactsList.size} contacts")
                _contacts.value = contactsList
            }
            result.onFailure { e ->
                android.util.Log.e("ContactsVM", "Load failed: ${e.message}")
                if (e.message?.contains("Unauthorized", ignoreCase = true) == true) {
                    _unauthorized.value = true
                } else {
                    _message.value = "Ошибка загрузки контактов: ${e.message}"
                }
            }
        }
    }

    private fun parseContacts(xml: String): List<Contact> {
        val contacts = mutableListOf<Contact>()

        val contactIdPattern = "<[^>]*contactId>([^<]*)</[^>]*contactId>".toRegex()
        val phonePattern = "<[^>]*phone>([^<]*)</[^>]*phone>".toRegex()
        val nicknamePattern = "<[^>]*nickname>([^<]*)</[^>]*nickname>".toRegex()

        val contactIds = contactIdPattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val phones = phonePattern.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val nicknames = nicknamePattern.findAll(xml).map { it.groupValues[1].trim() }.toList()

        for (i in contactIds.indices) {
            val phone = phones.getOrElse(i) { "" }
            if (phone.startsWith("+")) {
                val nickname = nicknames.getOrElse(i) { "" }
                val userId = try { java.util.UUID.fromString(contactIds[i]) } catch (_: Exception) { null }
                contacts.add(Contact(phone = phone, nickname = nickname, userId = userId))
            }
        }
        return contacts.sortedBy { it.nickname.ifBlank { it.phone } }
    }
}
