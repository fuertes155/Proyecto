package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.InvestmentMatch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentMatchRepositoryPort {
    InvestmentMatch save(InvestmentMatch match);
    List<InvestmentMatch> saveAll(List<InvestmentMatch> matches);
    Optional<InvestmentMatch> findById(UUID id);
    List<InvestmentMatch> findByBorrowerLoanId(UUID loanId);
    List<InvestmentMatch> findByFractionId(UUID fractionId);
}
