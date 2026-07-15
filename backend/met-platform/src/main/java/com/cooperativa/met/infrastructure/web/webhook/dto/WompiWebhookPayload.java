package com.cooperativa.met.infrastructure.web.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO que modela el payload de eventos reales enviados por Wompi.
 *
 * Documentación oficial: https://docs.wompi.co/docs/colombia/eventos/
 *
 * Ejemplo de evento "transaction.updated":
 * {
 *   "event": "transaction.updated",
 *   "data": {
 *     "transaction": {
 *       "id": "95288-1735000000-87654",
 *       "created_at": "2024-12-24T00:00:00.000Z",
 *       "amount_in_cents": 5000000,
 *       "reference": "MET-A1B2C3D4E5F6-1735000000000",
 *       "currency": "COP",
 *       "payment_method_type": "NEQUI",
 *       "status": "APPROVED",
 *       ...
 *     }
 *   },
 *   "sent_at": "2024-12-24T00:00:01.000Z"
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WompiWebhookPayload {

    private String event;
    private WompiData data;

    @JsonProperty("sent_at")
    private String sentAt;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiData {
        private WompiTransaction transaction;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiTransaction {

        /** ID único de la transacción en Wompi */
        private String id;

        /** Referencia que tu backend generó al crear el link de pago */
        private String reference;

        /** Monto en centavos (COP). Dividir por 100 para obtener pesos. */
        @JsonProperty("amount_in_cents")
        private Long amountInCents;

        /** Moneda: "COP" */
        private String currency;

        /**
         * Estado de la transacción:
         * - APPROVED: pago exitoso
         * - DECLINED: pago rechazado
         * - VOIDED: pago anulado
         * - ERROR: error en el proceso
         */
        private String status;

        /**
         * Método de pago utilizado:
         * - CARD: tarjeta de crédito/débito
         * - NEQUI: Nequi
         * - PSE: PSE
         * - BANCOLOMBIA_TRANSFER: Bre-B (antes Botón Bancolombia)
         * - BANCOLOMBIA_COLLECT: corresponsal bancario
         */
        @JsonProperty("payment_method_type")
        private String paymentMethodType;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("finalized_at")
        private String finalizedAt;

        /** Referencia de la entidad bancaria */
        @JsonProperty("merchant_message")
        private String merchantMessage;
    }
}
