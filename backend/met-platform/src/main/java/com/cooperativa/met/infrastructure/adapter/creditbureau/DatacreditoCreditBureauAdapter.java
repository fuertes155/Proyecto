package com.cooperativa.met.infrastructure.adapter.creditbureau;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.lending.model.CreditScoreResult;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

    private final WebClient datacreditoWebClient;

    @Override
    public CreditScoreResult checkScore(UUID userId, String nationalId, String firstName, String lastName, LocalDate dateOfBirth) {
        log.info("Realizando consulta a DataCrédito Experian para usuario {}", userId);
        
        try {
            // Ejemplo de Payload, la estructura exacta depende del contrato y API (Open Finance o Decisioning).
            Map<String, Object> requestBody = Map.of(
                    "identification", Map.of(
                            "type", "CC", // Cédula de ciudadanía
                            "number", nationalId
                    ),
                    "personalData", Map.of(
                            "firstName", firstName,
                            "lastName", lastName,
                            "dateOfBirth", dateOfBirth.toString()
                    )
            );

            // Llamada síncrona usando block()
            Map<String, Object> response = datacreditoWebClient.post()
                    .uri("/v1/credit-score/consult")
                    .bodyValue(requestBody)
                    // TODO: Aquí iría la inyección del Token OAuth o certificados requeridos por Experian
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

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
            throw new BusinessRuleException("CREDIT_BUREAU_UNAVAILABLE", "No fue posible consultar la central de riesgo en este momento.");
        }
    }
}
