package com.cooperativa.met.infrastructure.persistence.investment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentMatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestmentMatchJpaRepository extends JpaRepository<InvestmentMatchJpaEntity, UUID> {
    List<InvestmentMatchJpaEntity> findByBorrowerLoanId(UUID borrowerLoanId);
    List<InvestmentMatchJpaEntity> findByFractionId(UUID fractionId);
}
