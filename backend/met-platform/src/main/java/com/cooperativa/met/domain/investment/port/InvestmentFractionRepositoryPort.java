package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentFractionRepositoryPort {
    InvestmentFraction save(InvestmentFraction fraction);
    List<InvestmentFraction> saveAll(List<InvestmentFraction> fractions);
    Optional<InvestmentFraction> findById(UUID id);
    List<InvestmentFraction> findByStatus(InvestmentFractionStatus status);
    List<InvestmentFraction> findByOriginalDepositId(UUID depositId);
}
