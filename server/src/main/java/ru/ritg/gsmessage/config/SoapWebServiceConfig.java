package ru.ritg.gsmessage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Конфигурация SOAP Web Services.
 *
 * <p>Настраивает WSDL-эндпоинт, XSD-схему и SOAP-диспетчер на порту 8080.</p>
 */
@Configuration
public class SoapWebServiceConfig {

    /**
     * Создать SOAP-диспетчер.
     *
     * @return {@link MessageDispatcherServlet}
     */
    @Bean
    public MessageDispatcherServlet messageDispatcherServlet() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Создать WSDL-определение.
     *
     * @return {@link DefaultWsdl11Definition}
     */
    @Bean
    public DefaultWsdl11Definition defaultWsdl11Definition() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Загрузить XSD-схему.
     *
     * @return {@link XsdSchema}
     */
    @Bean
    public XsdSchema gsMessageSchema() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
