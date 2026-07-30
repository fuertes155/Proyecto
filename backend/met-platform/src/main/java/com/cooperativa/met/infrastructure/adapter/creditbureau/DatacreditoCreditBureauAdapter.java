package com.cooperativa.met.infrastructure.adapter.creditbureau;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.lending.model.CreditReportEvent;
import com.cooperativa.met.domain.lending.model.CreditScoreResult;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador Real para DataCrédito Experian Colombia.
 * Se activa cuando met.credit-bureau.provider=datacredito
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "met.credit-bureau.provider", havingValue = "datacredito")
@RequiredArgsConstructor
public class DatacreditoCreditBureauAdapter implements CreditBureauPort {

    private final RestClient datacreditoRestClient;

    @Override
    public CreditScoreResult checkScore(UUID userId, String nationalId, String firstName, String lastName,
            LocalDate dateOfBirth) {
        log.info("Realizando consulta a DataCrédito Experian para usuario {}", userId);

        try {
            // Ejemplo de Payload, la estructura exacta depende del contrato y API (Open
            // Finance o Decisioning).
            Map<String, Object> requestBody = Map.of(
                    "identification", Map.of(
                            "type", "CC", // Cédula de ciudadanía
                            "number", nationalId),
                    "personalData", Map.of(
                            "firstName", firstName,
                            "lastName", lastName,
                            "dateOfBirth", dateOfBirth.toString()));

            // Llamada síncrona usando RestClient. La autenticación OAuth2 (Bearer token)
            // se inyecta automáticamente vía interceptor — ver CreditBureauConfig / DatacreditoOAuthTokenProvider.
            Map response = datacreditoRestClient.post()
                    .uri("/v1/credit-score/consult")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("score")) {
                throw new BusinessRuleException("CREDIT_BUREAU_ERROR", "Respuesta inválida desde DataCrédito");
            }

            // Extraer datos del response map (asumiendo estructura JSON)
            int score = (Integer) response.get("score");
            String referenceId = (String) response.get("referenceId");

            return CreditScoreResult.builder()
                    .score(score)
                    .provider("DATACREDITO")
                    .referenceId(referenceId)
                    .queriedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Error consultando a DataCrédito Experian para usuario {}: {}", userId, e.getMessage());
            throw new BusinessRuleException("CREDIT_BUREAU_UNAVAILABLE",
                    "No fue posible consultar la central de riesgo en este momento.");
        }
    }

    @Override
    public void reportCreditBehavior(CreditReportEvent event) {
        log.info("Reportando comportamiento crediticio a DataCrédito Experian: préstamo {} evento {}",
                event.getLoanId(), event.getEventType());

        try {
            // TODO: Payload real depende del contrato de reporte de DataCrédito/Experian
            // (formato de archivo plano o API de reporte, según lo acordado con el proveedor).
            Map<String, Object> requestBody = Map.of(
                    "identification", Map.of("type", "CC", "number", event.getNationalId()),
                    "loanId", event.getLoanId().toString(),
                    "eventType", event.getEventType().name(),
                    "outstandingBalance", event.getOutstandingBalance(),
                    "daysLate", event.getDaysLate() != null ? event.getDaysLate() : 0,
                    "reportedAt", event.getReportedAt().toString());

            datacreditoRestClient.post()
                    .uri("/v1/credit-report/behavior")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // El reporte a la central de riesgo no debe bloquear el procesamiento de cobranza:
            // se registra el fallo para reintento posterior en lugar de propagar la excepción.
            log.error("Error reportando a DataCrédito Experian el préstamo {}: {}", event.getLoanId(), e.getMessage());
        }
    }
}
