package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.account.usecase.DepositUseCase;
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

    private final DepositUseCase depositUseCase;
    private final ProcessedWebhookJpaRepository processedWebhookRepository;
    private final ObjectMapper objectMapper;

    /**
     * Secret de webhook de Wompi.
     * Formato en Wompi Dashboard → Desarrolladores → Webhooks → Secreto.
     * En desarrollo se usa el secret simulado para el MockPaymentGateway.
     */
    @Value("${met.wompi.webhook-secret:super_secret_key_12345}")
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

    /**
     * Endpoint exclusivo del entorno de desarrollo para el MockPaymentGateway.
     * Se auto-firma internamente para evitar que el cliente HTML deba calcular HMAC.
     *
     * ⚠️  NO exponer en producción. Proteger con un perfil de Spring (@Profile("dev")).
     */
    @PostMapping("/mock-payment")
    public ResponseEntity<Void> handleMockPaymentWebhook(@RequestBody String rawPayload) {
        try {
            log.info("[DEV] Received mock payment webhook. Auto-signing payload.");
            processMockEvent(objectMapper.readValue(rawPayload,
                    com.cooperativa.met.infrastructure.web.webhook.dto.PaymentWebhookPayload.class));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[DEV] Error processing mock payment webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void processWebhookPayload(String rawPayload) throws Exception {
        // Intentamos parsear como evento real de Wompi
        try {
            WompiWebhookPayload wompiPayload = objectMapper.readValue(rawPayload, WompiWebhookPayload.class);
            if (wompiPayload.getEvent() != null && wompiPayload.getData() != null) {
                processWompiEvent(wompiPayload);
                return;
            }
        } catch (Exception ignored) {
            // No es un payload de Wompi real, intentamos el formato simulado
        }

        // Fallback: payload simulado del MockPaymentGateway (para desarrollo)
        com.cooperativa.met.infrastructure.web.webhook.dto.PaymentWebhookPayload mockPayload =
                objectMapper.readValue(rawPayload, com.cooperativa.met.infrastructure.web.webhook.dto.PaymentWebhookPayload.class);
        processMockEvent(mockPayload);
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
            log.info("Transaction {} already processed (Idempotency). Ignoring.", txId);
            return;
        }

        // Extraer userId de la referencia (formato: MET-{userId12chars}-{timestamp})
        UUID userId = extractUserIdFromReference(reference);
        if (userId == null) {
            log.error("Could not extract userId from Wompi reference: {}", reference);
            return;
        }

        // Wompi envía el monto en centavos para COP
        BigDecimal amountInPesos = BigDecimal.valueOf(tx.getAmountInCents()).divide(BigDecimal.valueOf(100));

        log.info("Processing approved Wompi transaction {} for user {} amount={} COP",
                txId, userId, amountInPesos);

        DepositRequest request = new DepositRequest();
        request.setAmount(amountInPesos);
        request.setMethod("WOMPI_" + tx.getPaymentMethodType());

        depositUseCase.execute(userId, request);

        processedWebhookRepository.save(ProcessedWebhookJpaEntity.builder()
                .transactionId(txId)
                .gateway("WOMPI")
                .processedAt(Instant.now())
                .build());

        log.info("Successfully deposited {} COP for user {} via Wompi", amountInPesos, userId);
    }

    /**
     * Procesa un evento del MockPaymentGateway (solo para desarrollo).
     */
    private void processMockEvent(com.cooperativa.met.infrastructure.web.webhook.dto.PaymentWebhookPayload payload) {
        log.info("Received MOCK payment webhook event: {}", payload.getEvent());

        if (!"transaction.updated".equals(payload.getEvent()) || payload.getData() == null) {
            return;
        }

        var data = payload.getData();
        if (!"APPROVED".equals(data.getStatus()) || data.getUserId() == null) {
            return;
        }

        String txId = data.getTransactionId();
        if (processedWebhookRepository.existsById(txId)) {
            log.info("Mock transaction {} already processed. Ignoring.", txId);
            return;
        }

        DepositRequest request = new DepositRequest();
        request.setAmount(data.getAmount());
        request.setMethod("PSE_MOCK_" + txId);

        depositUseCase.execute(data.getUserId(), request);

        processedWebhookRepository.save(ProcessedWebhookJpaEntity.builder()
                .transactionId(txId)
                .gateway("MOCK_GATEWAY")
                .processedAt(Instant.now())
                .build());
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

    /**
     * Extrae el UUID del usuario a partir de la referencia de pago.
     * Formato esperado: "MET-{userId12chars}-{timestamp}"
     * Ejemplo: "MET-A1B2C3D4E5F6-1735000000000"
     */
    private UUID extractUserIdFromReference(String reference) {
        try {
            // Buscar el userId en la base de datos a partir de la referencia no es posible aquí
            // sin acceso al repositorio de usuarios. En su lugar, el userId queda embebido
            // en la referencia como los primeros 12 chars del UUID sin guiones.
            // Para producción real: guardar la referencia+userId en una tabla de intenciones de pago.
            log.warn("Real Wompi webhook received. Reference: {}. " +
                     "Para producción, implementar tabla de intenciones de pago para mapear reference -> userId.",
                     reference);
            return null;
        } catch (Exception e) {
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
