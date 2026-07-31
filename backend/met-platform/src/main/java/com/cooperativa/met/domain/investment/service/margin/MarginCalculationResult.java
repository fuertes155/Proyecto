package com.cooperativa.met.domain.investment.service.margin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resultado transparente del cálculo de margen: deja explícito cuánto se
 * generó, cuánto se quedó la plataforma y cuánto le corresponde al inversionista,
 * para que el desglose sea auditable (no solo un número neto).
 */
@Getter
@Builder
public class MarginCalculationResult {
    private final MarginModel model;
    private final BigDecimal capitalBase;
    private final BigDecimal generatedInterest;
    private final BigDecimal marginRate;
    private final BigDecimal platformCommission;
    private final BigDecimal investorYield;
}
