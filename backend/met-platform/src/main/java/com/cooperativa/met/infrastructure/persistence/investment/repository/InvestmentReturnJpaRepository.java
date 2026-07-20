package com.cooperativa.met.infrastructure.persistence.investment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentReturnJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestmentReturnJpaRepository extends JpaRepository<InvestmentReturnJpaEntity, UUID> {

    List<InvestmentReturnJpaEntity> findByUserIdOrderByFechaPagoDesc(UUID userId);
}
