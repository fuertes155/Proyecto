package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.account.usecase.ProcessWebhookUseCase;
import com.cooperativa.met.infrastructure.persistence.webhook.entity.ProcessedWebhookJpaEntity;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
import com.cooperativa.met.infrastructure.web.webhook.dto.WompiWebhookPayload;
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
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

/**
 * Controlador público que recibe los eventos de pago de Wompi.
 *
 * Wompi envía una firma en el header "x-wompi-signature-v1" con el formato:
 *   SHA-256(timestamp + "." + payload + webhookSecret)
 *
 * Donde "timestamp" es el header "x-wompi-signature-v1" dividido por "t=" y "v1=".
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
     * Secret de webhook de Wompi.
     * Formato en Wompi Dashboard → Desarrolladores → Webhooks → Secreto.
     */
    @Value("${met.wompi.webhook-secret}")
    private String wompiWebhookSecret;

    /**
     * Endpoint principal que recibe los eventos de Wompi.
     * Documentación: https://docs.wompi.co/docs/colombia/eventos/
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestHeader(value = "x-wompi-signature-v1", required = false) String wompiSignatureHeader,
            @RequestHeader(value = "X-Signature", required = false) String legacySignatureHeader,
            @RequestBody String rawPayload) {

        try {
            boolean isValid;
            if (wompiSignatureHeader != null) {
                isValid = isValidWompiSignature(rawPayload, wompiSignatureHeader);
            } else if (legacySignatureHeader != null) {
                isValid = isValidLegacySignature(rawPayload, legacySignatureHeader);
            } else {
                log.warn("Missing webhook signature header. Rejecting request.");
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
            log.info("Transaction not approved. Status: {}", tx != null ? tx.getStatus() : "null");
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

        log.info("Processing approved Wompi transaction {} for user {} amount={} COP",
                txId, userId, amountInPesos);

        String method = "WOMPI_" + tx.getPaymentMethodType();
        processWebhookUseCase.execute(txId, "WOMPI", userId, amountInPesos, method);
        
        log.info("Successfully queued deposit of {} COP for user {} via Wompi", amountInPesos, userId);
    }



    /**
     * Verifica la firma real de Wompi (header: x-wompi-signature-v1).
     * Formato: "t={timestamp},v1={sha256hash}"
     *
     * Hash = SHA-256(timestamp + "." + rawBody + webhookSecret)
     */
    private boolean isValidWompiSignature(String payload, String signatureHeader) {
        try {
            // Parsear header: "t=1735000000,v1=abc123..."
            String timestamp = null;
            String receivedHash = null;
            for (String part : signatureHeader.split(",")) {
                if (part.startsWith("t=")) timestamp = part.substring(2);
                if (part.startsWith("v1=")) receivedHash = part.substring(3);
            }

            if (timestamp == null || receivedHash == null) {
                log.warn("Malformed Wompi signature header: {}", signatureHeader);
                return false;
            }

            String dataToHash = timestamp + "." + payload + wompiWebhookSecret;
            String expectedHash = sha256Hex(dataToHash);

            boolean valid = expectedHash.equals(receivedHash);
            if (!valid) {
                log.warn("Wompi signature mismatch. Expected: {}, Got: {}", expectedHash, receivedHash);
            }
            return valid;
        } catch (Exception e) {
            log.error("Error validating Wompi signature", e);
            return false;
        }
    }

    /**
     * Firma legada usada por el MockPaymentGateway (HMAC-SHA-256 en Base64).
     * Mantenemos compatibilidad para no romper el entorno de desarrollo.
     */
    private boolean isValidLegacySignature(String payload, String signatureHeader) {
        try {
            javax.crypto.Mac sha256HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey =
                    new javax.crypto.spec.SecretKeySpec(wompiWebhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hashBytes = sha256HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = java.util.Base64.getEncoder().encodeToString(hashBytes);
            return expectedSignature.equals(signatureHeader);
        } catch (Exception e) {
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

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
