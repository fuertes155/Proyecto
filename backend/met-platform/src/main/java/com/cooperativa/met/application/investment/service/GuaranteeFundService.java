package com.cooperativa.met.application.investment.service;

import com.cooperativa.met.domain.investment.model.GuaranteeFundMovement;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovementType;
import com.cooperativa.met.domain.investment.port.GuaranteeFundPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Fondo de Garantías/Aval Colectivo: se alimenta de cobros administrativos a
 * los deudores (ej: la tarifa FONDO_GARANTIAS descontada al desembolsar) y
 * cubre parcialmente la mora, según {@code guaranteeFundCoverageRatio}.
 *
 * Deliberadamente nunca cubre el 100% de una mora — eso debe quedar reflejado
 * aquí, en el sistema, no solo en el contrato.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuaranteeFundService {

    private final GuaranteeFundPort fundPort;

    @Transactional
    public GuaranteeFundMovement contribute(BigDecimal amount, String concept, UUID transactionReference) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El aporte al Fondo de Garantías debe ser positivo");
        }
        return fundPort.save(GuaranteeFundMovement.builder()
                .id(UUID.randomUUID())
                .type(GuaranteeFundMovementType.CONTRIBUTION)
                .amount(amount)
                .transactionReference(transactionReference)
                .concept(concept)
                .createdAt(Instant.now())
                .build());
    }

    /**
     * Calcula y registra cuánto puede cubrir el fondo de una mora, respetando
     * el tope de cobertura configurado y el saldo disponible. Nunca devuelve
     * más de {@code owedAmount * coverageRatio}, y nunca más de lo que el
     * fondo realmente tiene disponible.
     *
     * @return el monto efectivamente cubierto (puede ser menor al solicitado, o cero).
     */
    @Transactional
    public BigDecimal coverDefault(BigDecimal owedAmount, BigDecimal coverageRatio, UUID loanId, String concept) {
        BigDecimal desiredCoverage = owedAmount.multiply(coverageRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal available = fundPort.getBalance();
        BigDecimal actualCoverage = desiredCoverage.min(available);

        if (actualCoverage.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Fondo de Garantías sin saldo disponible para cubrir mora del préstamo {}", loanId);
            return BigDecimal.ZERO;
        }

        fundPort.save(GuaranteeFundMovement.builder()
                .id(UUID.randomUUID())
                .type(GuaranteeFundMovementType.PAYOUT)
                .amount(actualCoverage)
                .transactionReference(loanId)
                .concept(concept)
                .createdAt(Instant.now())
                .build());

        if (actualCoverage.compareTo(desiredCoverage) < 0) {
            log.warn("Fondo de Garantías cubrió parcialmente la mora del préstamo {}: solicitado={}, cubierto={} (saldo insuficiente)",
                    loanId, desiredCoverage, actualCoverage);
        }

        return actualCoverage;
    }

    public BigDecimal getBalance() {
        return fundPort.getBalance();
    }
}
