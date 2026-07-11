package com.cooperativa.met.infrastructure.web.webhook.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentWebhookPayload {
    private String event;
    private PaymentData data;

    @Data
    public static class PaymentData {
        private String transactionId;
        private BigDecimal amount;
        private String status; // e.g. "APPROVED"
        private UUID userId; // Mock simplification to link back to the user
    }
}
