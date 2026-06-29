package ru.ritg.messengerserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Конфигурация SOAP Web Services.
 *
 * <p>Настраивает WSDL-эндпоинт, XSD-схему и SOAP-диспетчер на порту 8080.</p>
 */
@Configuration
@EnableWs
public class SoapWebServiceConfig {

    /**
     * Создать и зарегистрировать SOAP-диспетчер на пути /messenger/*.
     *
     * @param applicationContext Spring-контекст для передачи в сервлет
     * @return {@link ServletRegistrationBean} с {@link MessageDispatcherServlet}
     */
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            @Autowired ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformSchemaLocations(true);
        return new ServletRegistrationBean<>(servlet, "/messenger/*");
    }

    /**
     * Создать WSDL-определение.
     *
     * @return {@link DefaultWsdl11Definition}
     */
    @Bean
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema messengerServerSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("MessengerPort");
        definition.setLocationUri("/messenger");
        definition.setTargetNamespace("http://ritg.ru/messengerserver");
        definition.setSchema(messengerServerSchema);
        return definition;
    }

    /**
     * Загрузить XSD-схему.
     *
     * @return {@link XsdSchema}
     */
    @Bean
    public XsdSchema messengerServerSchema() {
        return new SimpleXsdSchema(new org.springframework.core.io.ClassPathResource("schema.xsd"));
    }
}
