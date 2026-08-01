package com.cooperativa.met.infrastructure.messaging;

import com.cooperativa.met.domain.lending.port.MessagingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
public class TwilioSmsAdapter implements MessagingPort {

    private final RestTemplate restTemplate;

    @Value("${met.twilio.account-sid}")
    private String accountSid;

    @Value("${met.twilio.auth-token}")
    private String authToken;

    @Value("${met.twilio.phone-number}")
    private String fromPhoneNumber;

    @Value("${met.twilio.whatsapp-number}")
    private String fromWhatsappNumber;

    public TwilioSmsAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if ("mock-sid".equals(accountSid)) {
            log.info("[TWILIO MOCK SMS] Enviando SMS a {}: {}", phoneNumber, message);
            return;
        }
        sendMessage(phoneNumber, fromPhoneNumber, message);
    }

    @Override
    public void sendWhatsApp(String phoneNumber, String message) {
        if ("mock-sid".equals(accountSid)) {
            log.info("[TWILIO MOCK WHATSAPP] Enviando WA a {}: {}", phoneNumber, message);
            return;
        }
        sendMessage("whatsapp:" + phoneNumber, "whatsapp:" + fromWhatsappNumber, message);
    }

    private void sendMessage(String to, String from, String body) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = accountSid + ":" + authToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("To", to);
            map.add("From", from);
            map.add("Body", body);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("Mensaje enviado exitosamente a {} mediante Twilio", to);
        } catch (Exception e) {
            log.error("Error enviando mensaje por Twilio a {}", to, e);
        }
    }
}
