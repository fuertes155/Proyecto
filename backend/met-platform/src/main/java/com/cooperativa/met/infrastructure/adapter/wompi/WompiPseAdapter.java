package com.cooperativa.met.infrastructure.adapter.wompi;

import com.cooperativa.met.domain.bank.port.PseFinancialInstitution;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import com.cooperativa.met.domain.bank.port.PseTransactionRequest;
import com.cooperativa.met.domain.bank.port.PseTransactionResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Implementación de {@link PseGatewayPort} contra la API de Checkout/Transacciones
 * de Wompi (docs.wompi.co/docs/colombia/). Sirve igual para sandbox
 * ({@code https://sandbox.wompi.co/v1} + llaves {@code *_test_*}) y producción
 * ({@code https://production.wompi.co/v1} + llaves {@code *_prod_*}): solo cambian
 * la URL base y las llaves.
 *
 * <p>Flujo de una transacción PSE por API:</p>
 * <ol>
 *   <li>Tokens de aceptación vigentes: {@code GET /merchants/info} (header
 *       {@code x-merchant-public-key}). {@code GET /merchants/{llave}} queda solo como
 *       respaldo: Wompi lo retira el 31-oct-2026.</li>
 *   <li>{@code POST /transactions} con llave PRIVADA, firma de integridad,
 *       {@code acceptance_token} y {@code accept_personal_auth}.</li>
 *   <li>La URL del banco ({@code payment_method.extra.async_payment_url}) no viene en la
 *       respuesta de creación: se consulta {@code GET /transactions/{id}} (llave privada)
 *       hasta que aparece.</li>
 * </ol>
 *
 * <p>En sandbox el banco lo decide {@code financial_institution_code}: {@code "1"} aprueba,
 * {@code "2"} rechaza.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "met.payments.gateway", havingValue = "wompi", matchIfMissing = true)
@RequiredArgsConstructor
public class WompiPseAdapter implements PseGatewayPort {

    private static final String CURRENCY = "COP";
    private static final Set<String> FINAL_FAILED_STATUSES = Set.of("DECLINED", "ERROR", "VOIDED");

    private final RestClient wompiCheckoutRestClient;

    @Value("${met.wompi.public-key}")
    private String publicKey;

    @Value("${met.wompi.private-key}")
    private String privateKey;

    @Value("${met.wompi.integrity-secret}")
    private String integritySecret;

    @Value("${met.wompi.pse.poll-attempts:10}")
    private int pollAttempts;

    @Value("${met.wompi.pse.poll-interval-ms:1000}")
    private long pollIntervalMs;

    @Override
    @SuppressWarnings("unchecked")
    public List<PseFinancialInstitution> fetchFinancialInstitutions() {
        try {
            Map<String, Object> response = wompiCheckoutRestClient.get()
                    .uri("/pse/financial_institutions")
                    .header(HttpHeaders.AUTHORIZATION, bearer(publicKey))
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
        } catch (RestClientResponseException e) {
            log.error("Wompi respondió {} al listar bancos PSE: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessRuleException("WOMPI_PSE_BANKS_UNAVAILABLE",
                    "No fue posible consultar el catálogo de bancos PSE");
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
    public PseTransactionResult createPseTransaction(PseTransactionRequest request) {
        AcceptanceTokens tokens = fetchAcceptanceTokens();
        long amountInCents = request.amount().multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, Object> paymentMethod = new LinkedHashMap<>();
        paymentMethod.put("type", "PSE");
        paymentMethod.put("user_type", 0); // 0 = persona natural
        paymentMethod.put("user_legal_id_type", toWompiLegalIdType(request.documentType()));
        paymentMethod.put("user_legal_id", request.documentNumber());
        paymentMethod.put("financial_institution_code", request.financialInstitutionCode());
        paymentMethod.put("payment_description", request.description());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("acceptance_token", tokens.acceptanceToken());
        if (tokens.personalDataAuthToken() != null) {
            body.put("accept_personal_auth", tokens.personalDataAuthToken());
        }
        body.put("amount_in_cents", amountInCents);
        body.put("currency", CURRENCY);
        body.put("signature", WompiSignatures.integrity(request.reference(), amountInCents, CURRENCY, integritySecret));
        body.put("customer_email", request.customerEmail());
        body.put("reference", request.reference());
        body.put("redirect_url", request.redirectUrl());
        body.put("payment_method", paymentMethod);
        Map<String, Object> customerData = customerData(request);
        if (!customerData.isEmpty()) {
            body.put("customer_data", customerData);
        }

        Map<String, Object> transaction = callWompi("crear la transacción PSE", () ->
                dataOf(wompiCheckoutRestClient.post()
                        .uri("/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(privateKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class)));

        String transactionId = String.valueOf(transaction.get("id"));
        log.info("Transacción PSE creada en Wompi: id={} reference={} status={}",
                transactionId, request.reference(), transaction.get("status"));

        return waitForAsyncPaymentUrl(transactionId, transaction);
    }

    /**
     * La URL del banco aparece unos segundos después de crear la transacción; mientras
     * tanto la transacción sigue en PENDING sin {@code async_payment_url}.
     */
    private PseTransactionResult waitForAsyncPaymentUrl(String transactionId, Map<String, Object> transaction) {
        Map<String, Object> current = transaction;
        for (int attempt = 0; ; attempt++) {
            String status = String.valueOf(current.get("status"));
            if (FINAL_FAILED_STATUSES.contains(status)) {
                Object reason = current.get("status_message");
                log.warn("Wompi finalizó la transacción PSE {} con estado {}: {}", transactionId, status, reason);
                throw new BusinessRuleException("WOMPI_PSE_DECLINED",
                        "El pago PSE fue rechazado" + (reason != null ? ": " + reason : ""));
            }

            String asyncPaymentUrl = asyncPaymentUrlOf(current);
            if (asyncPaymentUrl != null) {
                return new PseTransactionResult(transactionId, asyncPaymentUrl, status);
            }

            if (attempt >= pollAttempts) {
                log.warn("Wompi no entregó async_payment_url para la transacción {} tras {} consultas",
                        transactionId, pollAttempts);
                throw new BusinessRuleException("WOMPI_PSE_TIMEOUT",
                        "El banco está tardando en responder. Intenta de nuevo en unos segundos.");
            }

            sleepBeforeNextPoll();
            current = callWompi("consultar la transacción PSE", () ->
                    dataOf(wompiCheckoutRestClient.get()
                            .uri("/transactions/{id}", transactionId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(privateKey))
                            .retrieve()
                            .body(Map.class)));
        }
    }

    private AcceptanceTokens fetchAcceptanceTokens() {
        Map<String, Object> merchant = callWompi("obtener los tokens de aceptación", () -> {
            try {
                return dataOf(wompiCheckoutRestClient.get()
                        .uri("/merchants/info")
                        .header("x-merchant-public-key", publicKey)
                        .retrieve()
                        .body(Map.class));
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                    throw e;
                }
                log.info("GET /merchants/info no disponible, se usa el endpoint anterior GET /merchants/<llave pública>");
                return dataOf(wompiCheckoutRestClient.get()
                        .uri("/merchants/{publicKey}", publicKey)
                        .retrieve()
                        .body(Map.class));
            }
        });

        String acceptanceToken = tokenOf(merchant, "presigned_acceptance");
        if (acceptanceToken == null) {
            throw new BusinessRuleException("WOMPI_PSE_ERROR", "No fue posible obtener el token de aceptación de Wompi");
        }
        return new AcceptanceTokens(acceptanceToken, tokenOf(merchant, "presigned_personal_data_auth"));
    }

    private static String tokenOf(Map<String, Object> merchant, String key) {
        if (merchant.get(key) instanceof Map<?, ?> presigned && presigned.get("acceptance_token") != null) {
            return presigned.get("acceptance_token").toString();
        }
        return null;
    }

    private static String asyncPaymentUrlOf(Map<String, Object> transaction) {
        if (transaction.get("payment_method") instanceof Map<?, ?> pm
                && pm.get("extra") instanceof Map<?, ?> extra
                && extra.get("async_payment_url") != null) {
            String url = extra.get("async_payment_url").toString();
            return url.isBlank() ? null : url;
        }
        return null;
    }

    private static Map<String, Object> customerData(PseTransactionRequest request) {
        Map<String, Object> customerData = new LinkedHashMap<>();
        if (request.customerFullName() != null && !request.customerFullName().isBlank()) {
            customerData.put("full_name", request.customerFullName().trim());
        }
        String phone = toWompiPhone(request.customerPhone());
        if (phone != null) {
            customerData.put("phone_number", phone);
        }
        return customerData;
    }

    /** Wompi espera el celular con indicativo de país y sin símbolos (p. ej. 573001234567). */
    static String toWompiPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10 && digits.startsWith("3")) {
            return "57" + digits;
        }
        return digits.isEmpty() ? null : digits;
    }

    /** Tipos de documento de Wompi: CC, CE, TI, NIT, PP (pasaporte)… */
    static String toWompiLegalIdType(String documentType) {
        return "PASAPORTE".equals(documentType) ? "PP" : documentType;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dataOf(Map<?, ?> response) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new BusinessRuleException("WOMPI_PSE_ERROR", "Respuesta inválida de Wompi");
        }
        return (Map<String, Object>) data;
    }

    /**
     * Ejecuta una llamada a Wompi y convierte sus errores HTTP en un mensaje legible:
     * en sandbox casi todo error es de validación (422) y el detalle viene en el cuerpo.
     */
    private static <T> T callWompi(String action, Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Wompi respondió {} al {}: {}", e.getStatusCode(), action, responseBody);
            throw new BusinessRuleException("WOMPI_PSE_ERROR",
                    "Wompi rechazó la solicitud al " + action + " (HTTP " + e.getStatusCode().value() + ")");
        }
    }

    private void sleepBeforeNextPoll() {
        if (pollIntervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException("WOMPI_PSE_TIMEOUT", "Se interrumpió la espera de la respuesta del banco");
        }
    }

    private static String bearer(String key) {
        return "Bearer " + key;
    }

    private record AcceptanceTokens(String acceptanceToken, String personalDataAuthToken) {
    }
}
