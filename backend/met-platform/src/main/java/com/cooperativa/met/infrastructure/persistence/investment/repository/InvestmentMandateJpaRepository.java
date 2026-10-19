package com.cooperativa.met.infrastructure.persistence.investment.repository;

import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentMandateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvestmentMandateJpaRepository extends JpaRepository<InvestmentMandateJpaEntity, UUID> {
}
