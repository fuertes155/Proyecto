package com.cooperativa.met.infrastructure.email;

import com.cooperativa.met.domain.notification.port.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envío de correo por la API HTTP de Brevo (https://api.brevo.com/v3/smtp/email).
 *
 * <p>Se activa con {@code met.mail.provider=brevo}. Pensado para Render, que
 * bloquea el SMTP saliente. El remitente ({@code met.mail.from-address}) debe
 * estar verificado en Brevo (Senders &amp; IP → Senders); sin dominio propio se
 * verifica el correo suelto (p. ej. el Gmail del proyecto) por email.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "met.mail.provider", havingValue = "brevo")
public class BrevoEmailSender implements EmailSenderPort {

    private final RestClient client;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailSender(
            @Value("${met.mail.brevo.api-key}") String apiKey,
            @Value("${met.mail.from-address}") String fromEmail,
            @Value("${met.mail.from-name:MET}") String fromName) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "met.mail.brevo.api-key (BREVO_API_KEY) es obligatorio cuando met.mail.provider=brevo");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.client = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey.trim())
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        log.info("Email provider: Brevo (from {} <{}>)", fromName, fromEmail);
    }

    @Override
    public void sendPlainText(String to, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", body
        );

        try {
            client.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String detail = new String(res.getBody().readAllBytes());
                        throw new EmailDeliveryException(
                            "Brevo respondió " + res.getStatusCode() + ": " + detail);
                    })
                    .toBodilessEntity();
            log.info("Brevo email sent to {}", to);
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailDeliveryException("Fallo llamando a la API de Brevo para " + to, e);
        }
    }
}
