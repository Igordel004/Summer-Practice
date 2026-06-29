package ru.ritg.messengerserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Сервис SMS-шлюза.
 *
 * <p>Отправляет OTP-коды через внешний SMS-провайдер sms.ru.</p>
 */
@Service
public class SmsGatewayService {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayService.class);
    private static final String SMS_TEMPLATE = "Ваш код подтверждения: %s";

    @Value("${sms.api-id}")
    private String apiId;

    @Value("${sms.api-url}")
    private String apiUrl;

    @Value("${sms.sender:SMS}")
    private String sender;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Отправить SMS с OTP-кодом.
     *
     * <p>В текущей реализации SMS не отправляется — код выводится в лог.
     * Для включения реальной отправки раскомментировать блок внутри метода.</p>
     *
     * @param phone номер телефона получателя
     * @param code  4-значный OTP-код
     * @return {@code true}
     */
    public boolean sendSms(String phone, String code) {
        String cleanPhone = phone.replace("+", "");
        log.info("SMS code for {}: {}", cleanPhone, code);
        return true;

/*
        String message = String.format(SMS_TEMPLATE, code);

        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("api_id", apiId)
                .queryParam("to", cleanPhone)
                .queryParam("msg", message)
                .queryParam("sender", sender)
                .queryParam("json", "1")
                .toUriString();

        log.info("Sending SMS to {} via sms.ru", cleanPhone);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("sms.ru response: {}", response.getBody());
            JsonNode root = objectMapper.readTree(response.getBody());

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.error("sms.ru returned status: {} for phone {}. Response: {}", status, cleanPhone, response.getBody());
                return false;
            }

            JsonNode smsNode = root.path("sms").path(cleanPhone);
            int smsStatusCode = smsNode.path("status_code").asInt(0);
            if (smsStatusCode != 100) {
                String smsStatusText = smsNode.path("status_text").asText("unknown error");
                log.error("SMS to {} failed with status_code {}: {}", cleanPhone, smsStatusCode, smsStatusText);
                return false;
            }

            log.info("SMS sent successfully to {}", cleanPhone);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", cleanPhone, e.getMessage());
            return false;
        }
*/
    }
}
