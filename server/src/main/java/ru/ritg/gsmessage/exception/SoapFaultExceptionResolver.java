package ru.ritg.gsmessage.exception;

import org.springframework.stereotype.Component;
import ru.ritg.gsmessage.dto.soap.SoapFault;

/**
 * Resolver для преобразования исключений в SOAP Fault.
 *
 * <p>Перехватывает runtime-исключения сервисного слоя и формирует
 * структурированный {@link SoapFault} с кодом и описанием.</p>
 */
@Component
public class SoapFaultExceptionResolver {

    /**
     * Преобразовать исключение в SOAP Fault.
     *
     * @param ex перехваченное исключение
     * @return {@link SoapFault} с кодом ошибки и сообщением
     */
    public SoapFault resolveException(Exception ex) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
