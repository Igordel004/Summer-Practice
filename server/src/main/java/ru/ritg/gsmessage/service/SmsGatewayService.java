package ru.ritg.gsmessage.service;

import org.springframework.stereotype.Service;

/**
 * Сервис SMS-шлюза.
 *
 * <p>Отправляет OTP-коды через внешний SMS-провайдер.
 * В текущей версии — заглушка (логирует в stdout).</p>
 */
@Service
public class SmsGatewayService {

    /**
     * Отправить SMS с OTP-кодом.
     *
     * <p>Входные данные: номер телефона и 4-значный код.</p>
     * <p>Параметры:</p>
     * <ul>
     *   <li>{@code phone} — номер получателя, E.164</li>
     *   <li>{@code code} — 4-значный OTP-код</li>
     * </ul>
     * <p>Ожидаемый результат: {@code true}, если SMS принято к отправке.</p>
     * <p>Возможные ошибки:</p>
     * <ul>
     *   <li>{@code SmsGatewayException} — ошибка на стороне провайдера</li>
     * </ul>
     *
     * @param phone номер телефона
     * @param code  OTP-код
     * @return флаг успеха отправки
     */
    public boolean sendSms(String phone, String code) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
