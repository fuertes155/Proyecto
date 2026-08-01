package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.bank.usecase.ProcessPayoutWebhookUseCase;
import com.cooperativa.met.infrastructure.web.webhook.dto.WompiPayoutWebhookPayload;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Recibe los eventos de "Pagos a Terceros" (payouts) de Wompi. Separado de
 * {@link WebhookController} (que atiende depósitos) porque son productos
 * distintos de Wompi con su propio secreto de firma — mezclar ambos en un
 * solo controlador arriesgaba confundir referencias de depósito y de payout.
 */
@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PayoutWebhookController {

    private final ProcessPayoutWebhookUseCase processPayoutWebhookUseCase;
    private final ObjectMapper objectMapper;

    @Value("${met.wompi.payouts.webhook-secret}")
    private String payoutWebhookSecret;

    @PostMapping("/payouts")
    public ResponseEntity<Void> handlePayoutWebhook(
            @RequestHeader(value = "x-wompi-signature-v1", required = false) String signatureHeader,
            @RequestBody String rawPayload) {

        try {
            if (signatureHeader == null || !isValidSignature(rawPayload, signatureHeader)) {
                log.warn("Firma de webhook de payouts ausente o inválida. Rechazando.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            WompiPayoutWebhookPayload payload = objectMapper.readValue(rawPayload, WompiPayoutWebhookPayload.class);
            if (payload.getData() == null || payload.getData().getTransaction() == null) {
                log.warn("Payload de webhook de payouts sin datos de transacción: {}", rawPayload);
                return ResponseEntity.badRequest().build();
            }

            WompiPayoutWebhookPayload.PayoutTransaction tx = payload.getData().getTransaction();
            String status = tx.getStatus() != null ? tx.getStatus().toUpperCase() : "";

            boolean approved = status.equals("APPROVED") || status.equals("SETTLED");
            boolean failed = status.equals("FAILED") || status.equals("REJECTED") || status.equals("DECLINED");

            if (!approved && !failed) {
                log.info("Evento de payout no terminal ignorado (status={}, reference={})", status, tx.getReference());
                return ResponseEntity.ok().build();
            }

            String failureCode = tx.getFailureReason() != null ? tx.getFailureReason().getCode() : null;
            String failureMessage = tx.getFailureReason() != null ? tx.getFailureReason().getMessage() : null;

            processPayoutWebhookUseCase.execute(tx.getReference(), approved, failureCode, failureMessage);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error procesando webhook de payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Misma firma que usa Wompi para el resto de sus webhooks: SHA-256(timestamp + "." + payload + secret). */
    private boolean isValidSignature(String payload, String signatureHeader) {
        try {
            String timestamp = null;
            String receivedHash = null;
            for (String part : signatureHeader.split(",")) {
                if (part.startsWith("t=")) timestamp = part.substring(2);
                if (part.startsWith("v1=")) receivedHash = part.substring(3);
            }
            if (timestamp == null || receivedHash == null) {
                log.warn("Header de firma de payouts malformado: {}", signatureHeader);
                return false;
            }

            String dataToHash = timestamp + "." + payload + payoutWebhookSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            boolean valid = MessageDigest.isEqual(
                    hexString.toString().getBytes(StandardCharsets.UTF_8),
                    receivedHash.getBytes(StandardCharsets.UTF_8));
            if (!valid) {
                log.warn("Firma de webhook de payouts no coincide");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error validando firma de webhook de payouts", e);
            return false;
        }
    }
}
