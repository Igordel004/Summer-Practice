package ru.ritg.messengerclient.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * WebSocket-клиент для обмена сообщениями в реальном времени.
 *
 * Устанавливает постоянное соединение с сервером по протоколу WS,
 * передаёт JSON-кадры и уведомляет слушателя о событиях.
 */
class WsClient {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var _open = false
    /** Слушатель событий WebSocket. */
    var listener: WsListener? = null

    /**
     * Интерфейс обратного вызова для событий WebSocket.
     */
    interface WsListener {
        /** Соединение установлено. */
        fun onConnected()
        /** Получено текстовое сообщение. */
        fun onMessage(json: String)
        /** Соединение закрыто. */
        fun onDisconnected(code: Int, reason: String)
        /** Ошибка соединения. */
        fun onError(error: String)
    }

    /**
     * Установить WebSocket-соединение с сервером.
     *
     * @param host хост сервера
     * @param port порт WebSocket
     * @param token JWT-токен для авторизации
     */
    fun connect(host: String, port: Int, token: String) {
        val url = "ws://$host:$port/ws/messages?token=$token"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _open = true
                listener?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener?.onMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _open = false
                webSocket.close(1000, null)
                listener?.onDisconnected(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _open = false
                listener?.onError(t.message ?: "WebSocket error")
            }
        })
    }

    /**
     * Отправить JSON-сообщение через WebSocket.
     *
     * @param json строка JSON
     */
    fun sendMessage(json: String) {
        webSocket?.send(json)
    }

    /** Закрыть WebSocket-соединение. */
    fun disconnect() {
        _open = false
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    /**
     * Проверить, установлено ли соединение.
     *
     * @return true, если соединение активно
     */
    fun isConnected(): Boolean = _open
}
