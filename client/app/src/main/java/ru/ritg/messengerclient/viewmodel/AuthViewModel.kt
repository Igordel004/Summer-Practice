package ru.ritg.messengerclient.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ritg.messengerclient.model.AppState
import ru.ritg.messengerclient.network.SoapClient

/**
 * ViewModel экрана авторизации.
 *
 * Управляет жизненным циклом OTP-кодов: запрос кода, верификация,
 * сохранение токена в SharedPreferences, обновление никнейма.
 * Периодически проверяет доступность сервера (ping).
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("messenger_client", Context.MODE_PRIVATE)
    private val soapClient = SoapClient()

    private val configHost: String by lazy {
        try {
            application.assets.open("server_config.properties").bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("server.host=") }
                    ?.substringAfter("=")
                    ?.trim()
                    ?: "10.0.2.2"
            }
        } catch (_: Exception) {
            "10.0.2.2"
        }
    }

    private val configPort: Int by lazy {
        try {
            application.assets.open("server_config.properties").bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("server.port=") }
                    ?.substringAfter("=")
                    ?.trim()
                    ?.toIntOrNull()
                    ?: 8080
            }
        } catch (_: Exception) {
            8080
        }
    }

    private val configWsPort: Int by lazy {
        try {
            application.assets.open("server_config.properties").bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("server.ws-port=") }
                    ?.substringAfter("=")
                    ?.trim()
                    ?.toIntOrNull()
                    ?: 8081
            }
        } catch (_: Exception) {
            8081
        }
    }

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _otpVerified = MutableStateFlow(false)
    val otpVerified: StateFlow<Boolean> = _otpVerified.asStateFlow()

    private val _pendingNickname = MutableStateFlow("")

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        val savedToken = prefs.getString("token", "") ?: ""
        val savedPhone = prefs.getString("phone", "") ?: ""
        if (savedToken.isNotEmpty()) {
            val savedNickname = prefs.getString("nickname", "") ?: ""
            val savedUserId = prefs.getString("userId", null)
            val userId = savedUserId?.let { try { java.util.UUID.fromString(it) } catch (_: Exception) { null } }
                ?: extractUserIdFromToken(savedToken)
            _state.value = AppState(token = savedToken, phone = savedPhone, nickname = savedNickname, userId = userId, serverHost = configHost, serverPort = configPort, wsPort = configWsPort)

            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) {
                    soapClient.configure(configHost, configPort)
                    soapClient.getContacts(savedToken)
                }
                result.onSuccess { xml ->
                    val fault = soapClient.extractSoapFault(xml)
                    if (fault.isNotBlank() && fault != "Unknown SOAP error") {
                        logout()
                    }
                }
                result.onFailure {
                    logout()
                }
            }
        } else {
            _state.value = AppState(serverHost = configHost, serverPort = configPort, wsPort = configWsPort)
        }

        viewModelScope.launch {
            while (true) {
                val s = _state.value
                val reachable = withContext(Dispatchers.IO) {
                    soapClient.configure(s.serverHost, s.serverPort)
                    soapClient.ping()
                }
                _isConnected.value = reachable
                delay(10_000)
            }
        }
    }

    /**
     * Обновить параметры подключения к серверу.
     *
     * @param host новый хост
     * @param port новый порт
     */
    fun updateServer(host: String, port: Int) {
        _state.value = _state.value.copy(serverHost = host, serverPort = port)
        soapClient.configure(host, port)
    }

    /**
     * Запросить OTP-код на номер телефона.
     *
     * @param phone номер телефона
     * @param nickname никнейм (при регистрации), пустая строка при входе
     */
    fun requestOtp(phone: String, nickname: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            _pendingNickname.value = nickname
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(_state.value.serverHost, _state.value.serverPort)
                soapClient.requestOtp(phone)
            }
            result.onSuccess { xml ->
                _state.value = _state.value.copy(phone = phone)
                _otpSent.value = true
                val success = soapClient.extractTag(xml, "success")
                if (success == "true") {
                    _message.value = "Код отправлен на $phone"
                } else {
                    val fault = soapClient.extractSoapFault(xml)
                    if (fault != "Unknown SOAP error") {
                        _message.value = "Ошибка: $fault"
                    } else {
                        _message.value = "Не удалось отправить SMS. Попробуйте позже."
                    }
                }
            }
            result.onFailure { e ->
                _message.value = "Ошибка сети: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    /**
     * Верифицировать OTP-код и получить JWT-токен.
     *
     * @param phone номер телефона
     * @param code 4-значный OTP-код
     */
    fun verifyOtp(phone: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null
            val nickname = _pendingNickname.value
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(_state.value.serverHost, _state.value.serverPort)
                soapClient.verifyOtp(phone, code, nickname)
            }
            result.onSuccess { xml ->
                val token = soapClient.extractNestedTag(xml, "AuthResponse", "token")
                val responseNickname = soapClient.extractNestedTag(xml, "AuthResponse", "nickname")
                if (token.isNotEmpty()) {
                    val userId = extractUserIdFromToken(token)
                    _state.value = _state.value.copy(token = token, phone = phone, nickname = responseNickname.ifBlank { _pendingNickname.value }, userId = userId)
                    prefs.edit().putString("token", token).putString("phone", phone).putString("nickname", responseNickname.ifBlank { _pendingNickname.value }).putString("userId", userId?.toString() ?: "").apply()
                    _otpVerified.value = true
                    _message.value = "Авторизация успешна"
                } else {
                    val fault = soapClient.extractSoapFault(xml)
                    _message.value = "Ошибка: $fault"
                }
            }
            result.onFailure { e ->
                _message.value = "Ошибка сети: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    /** Выйти из аккаунта: очистить состояние и удалить токен из SharedPreferences. */
    fun logout() {
        _state.value = AppState(serverHost = configHost, serverPort = configPort, wsPort = configWsPort)
        _otpSent.value = false
        _otpVerified.value = false
        _message.value = null
        prefs.edit().remove("token").remove("phone").remove("nickname").remove("userId").apply()
    }

    /** Сбросить состояние OTP-верификации. */
    fun resetOtpState() {
        _otpSent.value = false
        _otpVerified.value = false
        _message.value = null
    }

    /** Очистить сообщение об ошибке/успехе. */
    fun clearMessage() {
        _message.value = null
    }

    private fun extractUserIdFromToken(token: String): java.util.UUID? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
            val json = org.json.JSONObject(payload)
            val sub = json.optString("sub", "")
            if (sub.isNotEmpty()) java.util.UUID.fromString(sub) else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Обновить никнейм пользователя на сервере.
     *
     * @param newNickname новый никнейм
     * @param onResult callback (успех, сообщение)
     */
    fun updateNickname(newNickname: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                soapClient.configure(_state.value.serverHost, _state.value.serverPort)
                soapClient.updateNickname(_state.value.token, newNickname)
            }
            result.onSuccess { xml ->
                val success = soapClient.extractTag(xml, "success")
                if (success == "true") {
                    _state.value = _state.value.copy(nickname = newNickname)
                    prefs.edit().putString("nickname", newNickname).apply()
                    onResult(true, "Никнейм обновлён")
                } else {
                    onResult(false, "Ошибка обновления")
                }
            }
            result.onFailure { e ->
                onResult(false, "Ошибка сети: ${e.message}")
            }
            _isLoading.value = false
        }
    }
}
