package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MicroInvestmentPort {

    MicroInvestment save(MicroInvestment investment);

    Optional<MicroInvestment> findById(UUID id);

    List<MicroInvestment> findByPortfolioId(UUID portfolioId);

    List<MicroInvestment> findByUserId(UUID userId);

    /** Retorna todas las posiciones ACTIVE cuya fecha_vencimiento <= hoy (para maduración). */
    List<MicroInvestment> findMaturingOnOrBefore(LocalDate date, InvestmentStatus status);

    List<MicroInvestment> findByStatus(InvestmentStatus status);
}
