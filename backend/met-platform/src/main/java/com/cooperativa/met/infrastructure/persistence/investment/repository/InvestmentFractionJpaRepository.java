package com.cooperativa.met.infrastructure.persistence.investment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentFractionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentFractionJpaRepository extends JpaRepository<InvestmentFractionJpaEntity, UUID> {
    List<InvestmentFractionJpaEntity> findByStatus(String status);
    List<InvestmentFractionJpaEntity> findByOriginalDepositId(UUID originalDepositId);
    List<InvestmentFractionJpaEntity> findByInvestorAccountId(UUID investorAccountId);
}
