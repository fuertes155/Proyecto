package com.cooperativa.met.infrastructure.persistence.investment.repository;

import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentMatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentMatchJpaRepository extends JpaRepository<InvestmentMatchJpaEntity, UUID> {
    List<InvestmentMatchJpaEntity> findByBorrowerLoanId(UUID borrowerLoanId);
    List<InvestmentMatchJpaEntity> findByFractionId(UUID fractionId);
}
