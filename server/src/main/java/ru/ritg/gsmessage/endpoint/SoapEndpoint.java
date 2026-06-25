package ru.ritg.gsmessage.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import ru.ritg.gsmessage.dto.soap.*;
import ru.ritg.gsmessage.service.AuthService;
import ru.ritg.gsmessage.service.MessageRoutingService;
import ru.ritg.gsmessage.exception.SoapFaultExceptionResolver;

/**
 * SOAP Endpoint для обработки XML-запросов.
 *
 * <p>Обрабатывает запросы по пути SOAP / HTTP на порту 8080:</p>
 * <ul>
 *   <li>requestOtp — запрос OTP</li>
 *   <li>verifyOtp — верификация OTP и выдача JWT</li>
 *   <li>addContact — добавление контакта</li>
 *   <li>getHistory — получение истории переписки</li>
 * </ul>
 */
@Endpoint
public class SoapEndpoint {

    private final AuthService authService;
    private final MessageRoutingService messageRoutingService;
    private final SoapFaultExceptionResolver soapFaultResolver;

    public SoapEndpoint(AuthService authService,
                        MessageRoutingService messageRoutingService,
                        SoapFaultExceptionResolver soapFaultResolver) {
        this.authService = authService;
        this.messageRoutingService = messageRoutingService;
        this.soapFaultResolver = soapFaultResolver;
    }

    /**
     * Запросить отправку OTP-кода.
     *
     * <p>Входные данные: XML {@link RequestOtp}.</p>
     * <p>Параметры: {@code phone} — номер телефона, обязательный.</p>
     * <p>Ожидаемый результат: XML {@link ResponseOtp} — подтверждение.</p>
     * <p>Возможные ошибки: SOAP Fault InvalidPhone, TooManyRequests.</p>
     *
     * @param request запрос OTP
     * @return ответ с подтверждением
     */
    public ResponseOtp requestOtp(RequestOtp request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Верифицировать OTP-код и получить JWT-токен.
     *
     * <p>Входные данные: XML {@link VerifyOtp}.</p>
     * <p>Параметры: {@code phone}, {@code code}.</p>
     * <p>Ожидаемый результат: XML {@link AuthResponse} — JWT и срок действия.</p>
     * <p>Возможные ошибки: SOAP Fault InvalidOtp, OtpExpired.</p>
     *
     * @param request запрос верификации
     * @return ответ с JWT-токеном
     */
    public AuthResponse verifyOtp(VerifyOtp request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Добавить контакт в адресную книгу.
     *
     * <p>Входные данные: XML {@link AddContact}.</p>
     * <p>Параметры: {@code token}, {@code contactPhone}.</p>
     * <p>Ожидаемый результат: XML {@link ContactResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized, ContactNotFound.</p>
     *
     * @param request запрос добавления контакта
     * @return ответ с результатом
     */
    public ContactResponse addContact(AddContact request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Получить историю переписки с пагинацией.
     *
     * <p>Входные данные: XML {@link GetHistory}.</p>
     * <p>Параметры: {@code token}, {@code offset}, {@code limit}.</p>
     * <p>Ожидаемый результат: XML {@link HistoryResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized, InvalidOffset.</p>
     *
     * @param request запрос истории
     * @return ответ со списком сообщений
     */
    public HistoryResponse getHistory(GetHistory request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
