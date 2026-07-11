package com.cooperativa.met.infrastructure.web.webhook;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.account.usecase.DepositUseCase;
import com.cooperativa.met.infrastructure.web.webhook.dto.PaymentWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final DepositUseCase depositUseCase;

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePaymentWebhook(@RequestBody PaymentWebhookPayload payload) {
        log.info("Received payment webhook event: {}", payload.getEvent());

        if ("transaction.updated".equals(payload.getEvent()) && payload.getData() != null) {
            PaymentWebhookPayload.PaymentData data = payload.getData();
            
            if ("APPROVED".equals(data.getStatus()) && data.getUserId() != null) {
                log.info("Processing approved transaction {} for user {}", data.getTransactionId(), data.getUserId());
                
                DepositRequest request = new DepositRequest();
                request.setAmount(data.getAmount());
                request.setMethod("PSE_WEBHOOK_" + data.getTransactionId());
                
                // We use DepositUseCase directly to sum the balance
                // Security note: In a real environment, we MUST validate a Wompi/Bold signature header here
                depositUseCase.execute(data.getUserId(), request);
            }
        }

        return ResponseEntity.ok().build();
    }
}
