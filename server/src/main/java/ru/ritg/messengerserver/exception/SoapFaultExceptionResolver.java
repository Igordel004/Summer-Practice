package ru.ritg.messengerserver.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.soap.SoapFaultException;
import ru.ritg.messengerserver.dto.soap.SoapFault;

/**
 * Resolver для преобразования исключений в SOAP Fault.
 *
 * <p>Перехватывает runtime-исключения сервисного слоя и формирует
 * структурированный {@link SoapFault} с кодом и описанием.</p>
 */
@Component
public class SoapFaultExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(SoapFaultExceptionResolver.class);

    /**
     * Преобразовать исключение в SOAP Fault.
     *
     * @param ex перехваченное исключение
     * @return {@link SoapFault} с кодом ошибки и сообщением
     */
    public SoapFaultException resolveException(Exception ex) {
        String code;
        String message;

        if (ex instanceof InvalidPhoneException) {
            code = "InvalidPhone";
            message = ex.getMessage();
        } else if (ex instanceof InvalidOtpException) {
            code = "InvalidOtp";
            message = ex.getMessage();
        } else if (ex instanceof UnauthorizedException) {
            code = "Unauthorized";
            message = ex.getMessage();
        } else if (ex instanceof MessageNotFoundException) {
            code = "MessageNotFound";
            message = ex.getMessage();
        } else if (ex instanceof LimitExceededException) {
            code = "LimitExceeded";
            message = ex.getMessage();
        } else {
            code = "InternalError";
            message = "An unexpected error occurred";
            log.error("Unexpected exception", ex);
        }

        return new SoapFaultException(message);
    }
}
