package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.usecase.ProcessWebhookUseCase;
import com.cooperativa.met.infrastructure.adapter.wompi.WompiSignatures;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
import com.cooperativa.met.infrastructure.web.webhook.dto.WompiWebhookPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Controlador público que recibe los eventos de pago de Wompi
 * (docs.wompi.co/docs/colombia/eventos/).
 *
 * Wompi firma cada evento con un checksum SHA-256 que viene en el cuerpo
 * ({@code signature.checksum}) y repetido en el header {@code X-Event-Checksum}:
 * <pre>SHA256(valores de signature.properties + timestamp + secreto de eventos)</pre>
 * El secreto de eventos es {@code met.wompi.webhook-secret} ({@code test_events_…} en
 * sandbox, {@code prod_events_…} en producción).
 *
 * Wompi reintenta el envío (30 min, 3 h, 24 h) si no recibe un 200.
 *
 * IMPORTANTE: Este endpoint NO requiere autenticación JWT porque lo llama Wompi,
 * no el usuario de la app. La seguridad se garantiza con la verificación de la firma.
 */
@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ProcessWebhookUseCase processWebhookUseCase;
    private final ProcessedWebhookJpaRepository processedWebhookRepository;
    private final ObjectMapper objectMapper;

    /**
     * Secreto de eventos de Wompi: Dashboard → Desarrolladores → Secretos para
     * integración técnica → Eventos.
     */
    @Value("${met.wompi.webhook-secret}")
    private String wompiWebhookSecret;

    /**
     * Endpoint principal que recibe los eventos de Wompi.
     * URL a registrar en el dashboard de Wompi: {@code https://<host>/api/v1/webhooks/payment}
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestHeader(value = "X-Event-Checksum", required = false) String eventChecksumHeader,
            @RequestHeader(value = "X-Signature", required = false) String legacySignatureHeader,
            @RequestBody String rawPayload) {

        try {
            JsonNode event = objectMapper.readTree(rawPayload);
            String bodyChecksum = event.path("signature").path("checksum").asText(null);

            boolean isValid;
            if (eventChecksumHeader != null || bodyChecksum != null) {
                isValid = isValidWompiChecksum(event, eventChecksumHeader != null ? eventChecksumHeader : bodyChecksum);
            } else if (legacySignatureHeader != null) {
                isValid = isValidLegacySignature(rawPayload, legacySignatureHeader);
            } else {
                log.warn("Missing webhook signature (neither signature.checksum nor X-Signature). Rejecting request.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (!isValid) {
                log.warn("Invalid webhook signature. Rejecting request.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            processWebhookPayload(rawPayload);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void processWebhookPayload(String rawPayload) throws Exception {
        WompiWebhookPayload wompiPayload = objectMapper.readValue(rawPayload, WompiWebhookPayload.class);
        if (wompiPayload.getEvent() != null && wompiPayload.getData() != null) {
            processWompiEvent(wompiPayload);
        } else {
            log.warn("Payload is missing event or data fields");
            throw new IllegalArgumentException("Invalid Wompi payload");
        }
    }

    /**
     * Procesa un evento real de Wompi.
     * La transacción de Wompi usa centavos, así que dividimos por 100.
     */
    private void processWompiEvent(WompiWebhookPayload payload) {
        if (!"transaction.updated".equals(payload.getEvent())) {
            log.info("Ignoring unhandled Wompi event: {}", payload.getEvent());
            return;
        }

        WompiWebhookPayload.WompiTransaction tx = payload.getData().getTransaction();
        if (tx == null || !"APPROVED".equals(tx.getStatus())) {
            log.info("Wompi transaction {} not approved. Status: {} ({})",
                    tx != null ? tx.getReference() : "null",
                    tx != null ? tx.getStatus() : "null",
                    tx != null ? tx.getStatusMessage() : "");
            return;
        }

        if (tx.getCurrency() != null && !"COP".equals(tx.getCurrency())) {
            log.error("Ignoring Wompi transaction {} in unsupported currency {}", tx.getId(), tx.getCurrency());
            return;
        }

        String txId = tx.getId();
        String reference = tx.getReference();

        // Idempotencia: no procesar la misma transacción dos veces
        if (processedWebhookRepository.existsById(txId)) {
            log.info("Transaction {} already processed (Idempotency check).", txId);
            return;
        }

        // Extraer userId de la referencia (formato: MET-{userIdUUID}-{timestamp})
        UUID userId = extractUserIdFromReference(reference);
        if (userId == null) {
            log.error("Could not extract userId from Wompi reference: {}", reference);
            return;
        }

        // Wompi envía el monto en centavos para COP
        BigDecimal amountInPesos = BigDecimal.valueOf(tx.getAmountInCents()).divide(BigDecimal.valueOf(100));

        log.info("Processing approved Wompi transaction {} ({}) for user {} amount={} COP",
                txId, payload.getEnvironment(), userId, amountInPesos);

        String method = "WOMPI_" + tx.getPaymentMethodType();
        processWebhookUseCase.execute(txId, "WOMPI", userId, amountInPesos, method);

        log.info("Successfully queued deposit of {} COP for user {} via Wompi", amountInPesos, userId);
    }

    private boolean isValidWompiChecksum(JsonNode event, String receivedChecksum) {
        String expected = WompiSignatures.eventChecksum(event, wompiWebhookSecret);
        if (expected == null) {
            log.warn("Wompi event without usable signature.properties/timestamp: {}", event.path("signature"));
            return false;
        }
        boolean valid = WompiSignatures.checksumMatches(expected, receivedChecksum);
        if (!valid) {
            // Sin secretos en el log: solo qué propiedades firmó Wompi, para diagnosticar
            // un secreto de eventos equivocado (p. ej. el de producción en sandbox).
            log.warn("Wompi checksum mismatch. properties={} timestamp={} environment={}",
                    event.path("signature").path("properties"), event.path("timestamp"),
                    event.path("environment").asText("?"));
        }
        return valid;
    }

    /**
     * Firma legada HMAC-SHA-256 en Base64 (header X-Signature), usada para
     * pruebas manuales (colección de Postman). Wompi nunca la envía.
     */
    private boolean isValidLegacySignature(String payload, String signatureHeader) {
        try {
            javax.crypto.Mac sha256HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey =
                    new javax.crypto.spec.SecretKeySpec(wompiWebhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hashBytes = sha256HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = java.util.Base64.getEncoder().encodeToString(hashBytes);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error calculating legacy HMAC signature", e);
            return false;
        }
    }

    private UUID extractUserIdFromReference(String reference) {
        try {
            // Formato esperado: MET-{userIdUUID}-{timestamp}
            // Ejemplo: MET-550e8400-e29b-41d4-a716-446655440000-1735000000000
            String[] parts = reference.split("-");
            if (parts.length >= 6 && "MET".equals(parts[0])) {
                // Reconstruir el UUID
                String uuidString = parts[1] + "-" + parts[2] + "-" + parts[3] + "-" + parts[4] + "-" + parts[5];
                return UUID.fromString(uuidString);
            }
            log.error("Reference format is invalid: {}", reference);
            return null;
        } catch (Exception e) {
            log.error("Error extracting UUID from reference: {}", reference, e);
            return null;
        }
    }
}
