package com.cooperativa.met.infrastructure.web.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO provisional para los eventos de "Pagos a Terceros" de Wompi
 * (payout.updated / transaction.updated). Los nombres exactos de campos
 * deben confirmarse contra el payload real del sandbox una vez activadas
 * las credenciales — se modelan aquí según lo documentado públicamente:
 * evento con status terminal (APPROVED/FAILED) y, en caso de falla, un
 * objeto failureReason{code,message}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WompiPayoutWebhookPayload {

    private String event;
    private WompiPayoutData data;

    @JsonProperty("sent_at")
    private String sentAt;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiPayoutData {
        private PayoutTransaction transaction;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayoutTransaction {

        private String id;

        /** Referencia que WompiPayoutAdapter generó al crear el payout: "PAYOUT-{uuid}" */
        private String reference;

        /** Estado: APPROVED / FAILED / REJECTED / PENDING, etc. (por confirmar valores exactos) */
        private String status;

        @JsonProperty("failureReason")
        private FailureReason failureReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FailureReason {
        private String code;
        private String message;
    }
}
