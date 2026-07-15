package com.cooperativa.met.infrastructure.persistence.investment.repository;

import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentMandateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InvestmentMandateJpaRepository extends JpaRepository<InvestmentMandateJpaEntity, UUID> {
}
