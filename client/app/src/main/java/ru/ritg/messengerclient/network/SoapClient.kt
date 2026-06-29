package ru.ritg.messengerclient.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * SOAP-клиент для взаимодействия с сервером мессенджера.
 *
 * Формирует XML-запросы в обёртке SOAP 1.1 и отправляет их
 * на HTTP-эндпоинт сервера. Парсит XML-ответы с помощью регулярных выражений.
 *
 * @param host хост сервера (по умолчанию 10.0.2.2 — эмулятор Android)
 * @param port порт SOAP (по умолчанию 8080)
 */
class SoapClient(private var host: String = "10.0.2.2", private var port: Int = 8080) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val XML_TYPE = "text/xml; charset=utf-8".toMediaType()
    private val NAMESPACE = "http://ritg.ru/messengerserver"

    /**
     * Обновить параметры подключения к серверу.
     *
     * @param host новый хост
     * @param port новый порт
     */
    fun configure(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    private fun baseUrl() = "http://$host:$port/messenger"

    private fun envelope(body: String): String {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:msg=\"$NAMESPACE\">" +
            "<soapenv:Header/>" +
            "<soapenv:Body>$body</soapenv:Body>" +
            "</soapenv:Envelope>"
    }

    private fun soapAction(localPart: String) = "\"$NAMESPACE/$localPart\""

    /**
     * Запросить отправку OTP-кода на указанный номер телефона.
     *
     * @param phone номер телефона в формате E.164
     * @return XML-ответ сервера
     */
    fun requestOtp(phone: String): Result<String> {
        val xml = envelope("<msg:RequestOtp><msg:phone>$phone</msg:phone></msg:RequestOtp>")
        return postSoap(xml, "RequestOtp")
    }

    /**
     * Верифицировать OTP-код и получить JWT-токен.
     *
     * @param phone номер телефона
     * @param code 4-значный OTP-код
     * @param nickname никнейм (при регистрации), пустая строка при входе
     * @return XML-ответ с AuthResponse
     */
    fun verifyOtp(phone: String, code: String, nickname: String = ""): Result<String> {
        val nicknameXml = if (nickname.isNotBlank()) "<msg:nickname>$nickname</msg:nickname>" else ""
        val xml = envelope("<msg:VerifyOtp><msg:phone>$phone</msg:phone><msg:code>$code</msg:code>$nicknameXml</msg:VerifyOtp>")
        return postSoap(xml, "VerifyOtp")
    }

    /**
     * Добавить контакт по номеру телефона.
     *
     * @param token JWT-токен
     * @param contactPhone номер телефона контакта
     * @return XML-ответ с ContactResponse
     */
    fun addContact(token: String, contactPhone: String): Result<String> {
        val xml = envelope("<msg:AddContact><msg:token>$token</msg:token><msg:contactPhone>$contactPhone</msg:contactPhone></msg:AddContact>")
        return postSoap(xml, "AddContact")
    }

    /**
     * Удалить контакт по номеру телефона.
     *
     * @param token JWT-токен
     * @param contactPhone номер телефона контакта
     * @return XML-ответ с ContactResponse
     */
    fun deleteContact(token: String, contactPhone: String): Result<String> {
        val xml = envelope("<msg:DeleteContact><msg:token>$token</msg:token><msg:contactPhone>$contactPhone</msg:contactPhone></msg:DeleteContact>")
        return postSoap(xml, "DeleteContact")
    }

    /**
     * Получить историю переписки с пагинацией.
     *
     * @param token JWT-токен
     * @param contactId UUID собеседника
     * @param offset смещение
     * @param limit максимальное количество сообщений
     * @return XML-ответ с HistoryResponse
     */
    fun getHistory(token: String, contactId: java.util.UUID, offset: Int, limit: Int): Result<String> {
        val xml = envelope("<msg:GetHistory><msg:token>$token</msg:token><msg:contactId>$contactId</msg:contactId><msg:offset>$offset</msg:offset><msg:limit>$limit</msg:limit></msg:GetHistory>")
        return postSoap(xml, "GetHistory")
    }

    /**
     * Получить список контактов пользователя.
     *
     * @param token JWT-токен
     * @return XML-ответ с GetContactsResponse
     */
    fun getContacts(token: String): Result<String> {
        val xml = envelope("<msg:GetContacts><msg:token>$token</msg:token></msg:GetContacts>")
        return postSoap(xml, "GetContacts")
    }

    /**
     * Обновить никнейм пользователя.
     *
     * @param token JWT-токен
     * @param nickname новый никнейм
     * @return XML-ответ с UpdateNicknameResponse
     */
    fun updateNickname(token: String, nickname: String): Result<String> {
        val xml = envelope("<msg:UpdateNickname><msg:token>$token</msg:token><msg:nickname>$nickname</msg:nickname></msg:UpdateNickname>")
        return postSoap(xml, "UpdateNickname")
    }

    /**
     * Получить список чатов (собеседников).
     *
     * @param token JWT-токен
     * @return XML-ответ с GetChatsResponse
     */
    fun getChats(token: String): Result<String> {
        val xml = envelope("<msg:GetChats><msg:token>$token</msg:token></msg:GetChats>")
        return postSoap(xml, "GetChats")
    }

    private fun postSoap(xml: String, action: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(baseUrl())
                .addHeader("Content-Type", XML_TYPE.toString())
                .addHeader("SOAPAction", soapAction(action))
                .post(xml.toRequestBody(XML_TYPE))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Извлечь значение XML-тега из ответа.
     *
     * @param xml XML-строка
     * @param tag имя тега (без namespace)
     * @return текстовое содержимое тега или пустая строка
     */
    fun extractTag(xml: String, tag: String): String {
        val pattern = "<[^>]*:$tag[^>]*>([^<]*)</[^>]*:$tag>".toRegex()
        val match = pattern.find(xml)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    /**
     * Извлечь значение вложенного тега из XML.
     *
     * @param xml XML-строка
     * @param outerTag внешний тег
     * @param innerTag внутренний тег
     * @return текстовое содержимое внутреннего тега или пустая строка
     */
    fun extractNestedTag(xml: String, outerTag: String, innerTag: String): String {
        val outerPattern = "<[^:]*:$outerTag[^>]*>(.*?)</[^:]*:$outerTag>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val outerMatch = outerPattern.find(xml) ?: return ""
        val innerContent = outerMatch.groupValues[1]
        val innerPattern = "<[^:]*:$innerTag[^>]*>([^<]*)</[^:]*:$innerTag>".toRegex()
        val innerMatch = innerPattern.find(innerContent)
        return innerMatch?.groupValues?.get(1)?.trim() ?: ""
    }

    /**
     * Извлечь сообщение ошибки из SOAP Fault.
     *
     * @param xml XML-ответ с SOAP Fault
     * @return строка ошибки или «Unknown SOAP error»
     */
    fun extractSoapFault(xml: String): String {
        val faultPattern = "<soapenv:Fault>.*?<faultstring>([^<]*)</faultstring>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = faultPattern.find(xml)
        return match?.groupValues?.get(1) ?: "Unknown SOAP error"
    }

    /**
     * Проверить доступность сервера (ping).
     *
     * @return true, если сервер отвечает
     */
    fun ping(): Boolean {
        return try {
            val pingClient = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("http://$host:$port/")
                .get()
                .build()
            val response = pingClient.newCall(request).execute()
            response.close()
            true
        } catch (_: Exception) {
            false
        }
    }
}
