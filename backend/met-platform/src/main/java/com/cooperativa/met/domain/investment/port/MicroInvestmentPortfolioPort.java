package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MicroInvestmentPortfolioPort {

    MicroInvestmentPortfolio save(MicroInvestmentPortfolio portfolio);

    Optional<MicroInvestmentPortfolio> findById(UUID id);

    List<MicroInvestmentPortfolio> findByUserId(UUID userId);
}
