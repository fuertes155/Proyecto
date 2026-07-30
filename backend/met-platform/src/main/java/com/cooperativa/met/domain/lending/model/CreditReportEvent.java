package com.cooperativa.met.domain.lending.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento de comportamiento crediticio a reportar a la central de riesgo:
 * la contraparte del {@link com.cooperativa.met.domain.lending.port.CreditBureauPort#checkScore}
 * (que consulta) en el ciclo de vida crediticia.
 */
@Getter
@Builder
public class CreditReportEvent {

    private final UUID userId;
    private final String nationalId;
    private final UUID loanId;
    private final CreditReportEventType eventType;
    private final BigDecimal outstandingBalance;
    /** Días de mora al momento del reporte. Null salvo en eventos de tipo MORA. */
    private final Integer daysLate;
    private final LocalDate reportedAt;
}
