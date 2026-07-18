package com.cooperativa.met.infrastructure.adapter.creditbureau;

import com.cooperativa.met.domain.lending.model.CreditScoreResult;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Adaptador Mock (Sandbox) para desarrollo y QA.
 * Genera scores deterministas basados en el número de documento.
 * Se activa por defecto o cuando met.credit-bureau.provider=mock
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "met.credit-bureau.provider", havingValue = "mock", matchIfMissing = true)
public class MockCreditBureauAdapter implements CreditBureauPort {

    @Override
    public CreditScoreResult checkScore(UUID userId, String nationalId, String firstName, String lastName, LocalDate dateOfBirth) {
        log.info("Mocking credit score check for user {} (nationalId: {})", userId, nationalId);
        
        // Simulación determinista: generamos un score de 300 a 950 basado en el hash del nationalId
        int hash = Math.abs(nationalId.hashCode());
        int simulatedScore = 300 + (hash % 651); // (950 - 300 + 1) = 651
        
        // Excepciones para QA:
        // Si la cédula termina en "999", forzamos rechazo con un score de 400.
        // Si la cédula termina en "000", forzamos aprobación excelente con 900.
        if (nationalId.endsWith("999")) {
            simulatedScore = 400;
        } else if (nationalId.endsWith("000")) {
            simulatedScore = 900;
        }

        return CreditScoreResult.builder()
                .score(simulatedScore)
                .provider("MOCK")
                .referenceId("MOCK-" + UUID.randomUUID().toString())
                .queriedAt(Instant.now())
                .build();
    }
}
