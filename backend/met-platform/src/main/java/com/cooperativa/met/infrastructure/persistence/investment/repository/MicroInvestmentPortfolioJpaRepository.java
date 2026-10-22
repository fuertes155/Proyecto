package com.cooperativa.met.infrastructure.persistence.investment.repository;

import com.cooperativa.met.infrastructure.persistence.investment.entity.MicroInvestmentPortfolioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MicroInvestmentPortfolioJpaRepository extends JpaRepository<MicroInvestmentPortfolioJpaEntity, UUID> {

    List<MicroInvestmentPortfolioJpaEntity> findByUserId(UUID userId);
}
