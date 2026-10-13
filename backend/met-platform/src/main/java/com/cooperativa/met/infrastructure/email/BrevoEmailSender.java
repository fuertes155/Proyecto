package com.cooperativa.met.infrastructure.email;

import com.cooperativa.met.domain.notification.port.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envío de correo por la API HTTP de Brevo (https://api.brevo.com/v3/smtp/email).
 *
 * <p>Se activa con {@code met.mail.provider=brevo}. Pensado para Render, que
 * bloquea el SMTP saliente. Usa el {@link HttpClient} del JDK (sin cargar el
 * stack de cliente web de Spring) para no inflar la memoria en instancias
 * chicas.
 *
 * <p>El remitente ({@code met.mail.from-address}) debe estar verificado en
 * Brevo (Senders); sin dominio propio se verifica el correo suelto por email.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "met.mail.provider", havingValue = "brevo")
public class BrevoEmailSender implements EmailSenderPort {

    private static final URI ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailSender(
            @Value("${met.mail.brevo.api-key:}") String apiKey,
            @Value("${met.mail.from-address}") String fromEmail,
            @Value("${met.mail.from-name:MET}") String fromName) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "met.mail.brevo.api-key (BREVO_API_KEY) es obligatorio cuando met.mail.provider=brevo");
        }
        this.apiKey = apiKey.trim();
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        log.info("Email provider: Brevo (from {} <{}>)", fromName, fromEmail);
    }

    @Override
    public void sendPlainText(String to, String subject, String body) {
        String payload = "{"
                + "\"sender\":{\"name\":" + jsonStr(fromName) + ",\"email\":" + jsonStr(fromEmail) + "},"
                + "\"to\":[{\"email\":" + jsonStr(to) + "}],"
                + "\"subject\":" + jsonStr(subject) + ","
                + "\"textContent\":" + jsonStr(body)
                + "}";

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new EmailDeliveryException("Fallo llamando a la API de Brevo para " + to, e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new EmailDeliveryException(
                "Brevo respondió " + response.statusCode() + ": " + response.body());
        }
        log.info("Brevo email sent to {}", to);
    }

    /** Escapa un string como literal JSON (comillas incluidas). */
    private static String jsonStr(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
