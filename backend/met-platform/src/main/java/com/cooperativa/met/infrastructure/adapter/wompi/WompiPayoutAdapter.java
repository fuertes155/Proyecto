package com.cooperativa.met.infrastructure.adapter.wompi;

import com.cooperativa.met.domain.bank.port.PayoutGatewayBank;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayRequest;
import com.cooperativa.met.domain.bank.port.PayoutGatewayResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementación de {@link PayoutGatewayPort} contra la API de "Pagos a
 * Terceros" de Wompi (docs.wompi.co/en/docs/colombia/introduccion-pagos-a-terceros).
 *
 * <p><b>Campos y forma del payload son provisionales</b>: se basan en la
 * documentación pública consultada (endpoints POST /payouts, GET /banks;
 * campos identification{type,number}, bankId, accountType AHORROS/CORRIENTE,
 * accountNumber, beneficiaryName/Email, amountInCents, reference,
 * sourceAccountId, paymentType). Antes de ir a producción, verificar el
 * nombre exacto de cada campo/endpoint contra el sandbox real una vez
 * activadas las credenciales — no se han probado contra la API en vivo.</p>
 */
@Slf4j
@Component
@Profile("!dev")
@RequiredArgsConstructor
public class WompiPayoutAdapter implements PayoutGatewayPort {

    private final RestClient wompiPayoutRestClient;

    /** Cuenta de origen (Wompi/Bancolombia) desde la que se fondean los payouts. Ver GET /accounts. */
    @Value("${met.wompi.payouts.source-account-id}")
    private String sourceAccountId;

    @Override
    @SuppressWarnings("unchecked")
    public List<PayoutGatewayBank> fetchSupportedBanks() {
        try {
            List<Map<String, Object>> data = wompiPayoutRestClient.get()
                    .uri("/banks")
                    .retrieve()
                    .body(List.class);

            if (data == null) {
                return List.of();
            }
            return data.stream()
                    .map(this::toGatewayBank)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Error consultando catálogo de bancos en Wompi Payouts: {}", e.getMessage(), e);
            throw new BusinessRuleException("WOMPI_BANKS_UNAVAILABLE",
                    "No fue posible consultar el catálogo de bancos del proveedor de pagos");
        }
    }

    private PayoutGatewayBank toGatewayBank(Map<String, Object> raw) {
        Object id = raw.getOrDefault("id", raw.get("bankId"));
        Object name = raw.get("name");
        if (id == null || name == null) {
            log.warn("Entrada de banco sin id/name en la respuesta de Wompi, se omite: {}", raw);
            return null;
        }
        return new PayoutGatewayBank(id.toString(), name.toString(), true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PayoutGatewayResult initiatePayout(PayoutGatewayRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference", request.reference());
        body.put("amountInCents", request.amount().multiply(BigDecimal.valueOf(100)).longValueExact());
        body.put("bankId", request.destinationWompiBankId());
        body.put("accountType", mapAccountType(request.destinationAccountType()));
        body.put("accountNumber", request.destinationAccountNumber());
        body.put("identification", Map.of(
                "type", request.beneficiaryDocumentType(),
                "number", request.beneficiaryDocumentNumber()));
        body.put("beneficiaryName", request.beneficiaryFullName());
        body.put("beneficiaryEmail", request.beneficiaryEmail());
        body.put("sourceAccountId", sourceAccountId);
        body.put("paymentType", "OTHER");

        try {
            Map<String, Object> response = wompiPayoutRestClient.post()
                    .uri("/payouts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String railReference = response != null && response.get("id") != null
                    ? response.get("id").toString()
                    : request.reference();

            log.info("Payout {} aceptado por Wompi para procesamiento, railReference={}",
                    request.internalPayoutId(), railReference);
            return new PayoutGatewayResult(railReference, PayoutGatewayResult.PayoutGatewayStatus.ACCEPTED, null, null);

        } catch (HttpStatusCodeException e) {
            String errorBody = e.getResponseBodyAsString();
            log.warn("Wompi rechazó el payout {} de forma síncrona (status={}): {}",
                    request.internalPayoutId(), e.getStatusCode(), errorBody);
            return new PayoutGatewayResult(null, PayoutGatewayResult.PayoutGatewayStatus.REJECTED,
                    "PROVIDER_" + e.getStatusCode().value(), errorBody);
        }
    }

    private String mapAccountType(String domainAccountType) {
        return "SAVINGS".equals(domainAccountType) ? "AHORROS" : "CORRIENTE";
    }
}
