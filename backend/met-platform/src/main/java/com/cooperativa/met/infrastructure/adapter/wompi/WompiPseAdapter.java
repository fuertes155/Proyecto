package com.cooperativa.met.infrastructure.adapter.wompi;

import com.cooperativa.met.domain.bank.port.PseFinancialInstitution;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import com.cooperativa.met.domain.bank.port.PseTransactionRequest;
import com.cooperativa.met.domain.bank.port.PseTransactionResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementación de {@link PseGatewayPort} contra la API pública de
 * Checkout/Transacciones de Wompi (docs.wompi.co/docs/colombia/). A
 * diferencia de {@code WompiPayoutAdapter} (producto "Pagos a Terceros",
 * poco documentado), esta es la API principal y más documentada de Wompi
 * — usada aquí para: (1) listar bancos PSE reales
 * (GET /pse/financial_institutions) y (2) crear la transacción PSE
 * directamente vía API en vez de redirigir al checkout hosteado, usando
 * el flujo estándar de Wompi: obtener un acceptance_token vigente
 * (GET /merchants/{publicKey}) y luego POST /transactions con
 * payment_method.type = "PSE".
 *
 * Aun así, verificar contra el sandbox real antes de producción — no se
 * ha probado en vivo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WompiPseAdapter implements PseGatewayPort {

    private final RestClient wompiCheckoutRestClient;

    @Value("${met.wompi.public-key}")
    private String publicKey;

    @Override
    @SuppressWarnings("unchecked")
    public List<PseFinancialInstitution> fetchFinancialInstitutions() {
        try {
            Map<String, Object> response = wompiCheckoutRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/pse/financial_institutions")
                            .queryParam("public-key", publicKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !(response.get("data") instanceof List<?> data)) {
                return List.of();
            }

            return data.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .map(this::toFinancialInstitution)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Error consultando instituciones financieras PSE en Wompi: {}", e.getMessage(), e);
            throw new BusinessRuleException("WOMPI_PSE_BANKS_UNAVAILABLE",
                    "No fue posible consultar el catálogo de bancos PSE");
        }
    }

    private PseFinancialInstitution toFinancialInstitution(Map<String, Object> raw) {
        Object code = raw.get("financial_institution_code");
        Object name = raw.get("financial_institution_name");
        if (code == null || name == null) {
            log.warn("Institución PSE sin code/name en la respuesta de Wompi, se omite: {}", raw);
            return null;
        }
        return new PseFinancialInstitution(code.toString(), name.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public PseTransactionResult createPseTransaction(PseTransactionRequest request) {
        String acceptanceToken = fetchAcceptanceToken();

        Map<String, Object> paymentMethod = new LinkedHashMap<>();
        paymentMethod.put("type", "PSE");
        paymentMethod.put("user_type", 0);
        paymentMethod.put("user_legal_id_type", request.documentType());
        paymentMethod.put("user_legal_id", request.documentNumber());
        paymentMethod.put("financial_institution_code", request.financialInstitutionCode());
        paymentMethod.put("payment_description", request.description());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("acceptance_token", acceptanceToken);
        body.put("amount_in_cents", request.amount().multiply(BigDecimal.valueOf(100)).longValueExact());
        body.put("currency", "COP");
        body.put("customer_email", request.customerEmail());
        body.put("reference", request.reference());
        body.put("redirect_url", request.redirectUrl());
        body.put("payment_method", paymentMethod);

        Map<String, Object> response = wompiCheckoutRestClient.post()
                .uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("data") instanceof Map<?, ?> rawData)) {
            throw new BusinessRuleException("WOMPI_PSE_ERROR", "Respuesta inválida al crear la transacción PSE");
        }
        Map<String, Object> data = (Map<String, Object>) rawData;

        String transactionId = String.valueOf(data.get("id"));
        String status = String.valueOf(data.get("status"));
        String asyncPaymentUrl = null;
        if (data.get("payment_method") instanceof Map<?, ?> pm && pm.get("extra") instanceof Map<?, ?> extra) {
            Object url = extra.get("async_payment_url");
            asyncPaymentUrl = url != null ? url.toString() : null;
        }

        return new PseTransactionResult(transactionId, asyncPaymentUrl, status);
    }

    @SuppressWarnings("unchecked")
    private String fetchAcceptanceToken() {
        Map<String, Object> response = wompiCheckoutRestClient.get()
                .uri("/merchants/" + publicKey)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("data") instanceof Map<?, ?> data)
                || !(data.get("presigned_acceptance") instanceof Map<?, ?> presigned)
                || presigned.get("acceptance_token") == null) {
            throw new BusinessRuleException("WOMPI_PSE_ERROR", "No fue posible obtener el token de aceptación de Wompi");
        }
        return presigned.get("acceptance_token").toString();
    }
}
