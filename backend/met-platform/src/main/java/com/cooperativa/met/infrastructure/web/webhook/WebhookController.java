package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.account.usecase.DepositUseCase;
import com.cooperativa.met.infrastructure.persistence.webhook.entity.ProcessedWebhookJpaEntity;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
import com.cooperativa.met.infrastructure.web.webhook.dto.PaymentWebhookPayload;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final DepositUseCase depositUseCase;
    private final ProcessedWebhookJpaRepository processedWebhookRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.secret:super_secret_key_12345}")
    private String webhookSecret;

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signatureHeader,
            @RequestBody String rawPayload) {
        
        try {
            // 1. Validate Signature (HMAC SHA-256)
            if (signatureHeader == null || !isValidSignature(rawPayload, signatureHeader)) {
                log.warn("Invalid or missing webhook signature. Rejecting request.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Parse payload
            PaymentWebhookPayload payload = objectMapper.readValue(rawPayload, PaymentWebhookPayload.class);
            log.info("Received payment webhook event: {}", payload.getEvent());

            if ("transaction.updated".equals(payload.getEvent()) && payload.getData() != null) {
                PaymentWebhookPayload.PaymentData data = payload.getData();
                
                if ("APPROVED".equals(data.getStatus()) && data.getUserId() != null) {
                    String txId = data.getTransactionId();

                    // 2. Idempotency Check
                    if (processedWebhookRepository.existsById(txId)) {
                        log.info("Transaction {} already processed (Idempotency Key Hit). Ignoring duplicate.", txId);
                        return ResponseEntity.ok().build();
                    }

                    log.info("Processing approved transaction {} for user {}", txId, data.getUserId());
                    
                    DepositRequest request = new DepositRequest();
                    request.setAmount(data.getAmount());
                    request.setMethod("PSE_WEBHOOK_" + txId);
                    
                    // 3. Process Deposit (which must be Transactional)
                    depositUseCase.execute(data.getUserId(), request);

                    // 4. Mark as processed
                    processedWebhookRepository.save(ProcessedWebhookJpaEntity.builder()
                            .transactionId(txId)
                            .gateway("GENERIC_GATEWAY")
                            .processedAt(Instant.now())
                            .build());
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            // Return 200 OK or 500 depending on gateway retry strategy. Usually 500 triggers a retry.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean isValidSignature(String payload, String signatureHeader) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);

            byte[] hashBytes = sha256HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hashBytes);

            return expectedSignature.equals(signatureHeader);
        } catch (Exception e) {
            log.error("Error calculating HMAC signature", e);
            return false;
        }
    }
}
