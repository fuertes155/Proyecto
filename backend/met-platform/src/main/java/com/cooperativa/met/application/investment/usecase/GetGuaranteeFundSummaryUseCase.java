package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.dto.GuaranteeFundMovementResponse;
import com.cooperativa.met.application.investment.dto.GuaranteeFundSummaryResponse;
import com.cooperativa.met.application.investment.service.GuaranteeFundService;
import com.cooperativa.met.domain.investment.port.GuaranteeFundPort;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Vista de administrador sobre el Fondo de Garantías/Aval Colectivo: saldo
 * actual, las reglas que lo gobiernan (cobertura máxima y días de mora que la
 * activan) y el historial de movimientos. Antes de esto, el único modo de
 * ver el saldo era consultando la base de datos directamente.
 */
@Service
@RequiredArgsConstructor
public class GetGuaranteeFundSummaryUseCase {

    private final GuaranteeFundService guaranteeFundService;
    private final GuaranteeFundPort guaranteeFundPort;
    private final CapitalEngineProperties capitalEngineProperties;

    public GuaranteeFundSummaryResponse execute() {
        var movements = guaranteeFundPort.findAllOrderByCreatedAtDesc().stream()
                .map(m -> new GuaranteeFundMovementResponse(
                        m.getId(), m.getType().name(), m.getAmount(),
                        m.getTransactionReference(), m.getConcept(), m.getCreatedAt()))
                .toList();

        return new GuaranteeFundSummaryResponse(
                guaranteeFundService.getBalance(),
                capitalEngineProperties.getGuaranteeFundCoverageRatio(),
                capitalEngineProperties.getGuaranteeFundActivationDaysLate(),
                movements);
    }
}
