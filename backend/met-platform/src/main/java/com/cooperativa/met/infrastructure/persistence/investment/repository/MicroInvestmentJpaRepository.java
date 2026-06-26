package com.cooperativa.met.infrastructure.persistence.investment.repository;

import com.cooperativa.met.infrastructure.persistence.investment.entity.MicroInvestmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MicroInvestmentJpaRepository extends JpaRepository<MicroInvestmentJpaEntity, UUID> {

    List<MicroInvestmentJpaEntity> findByPortfolioId(UUID portfolioId);

    List<MicroInvestmentJpaEntity> findByUserId(UUID userId);

    @Query("SELECT m FROM MicroInvestmentJpaEntity m " +
           "WHERE m.estado = :estado AND m.fechaVencimiento <= :fecha")
    List<MicroInvestmentJpaEntity> findMaturingOnOrBefore(
            @Param("fecha") LocalDate fecha,
            @Param("estado") String estado);
}
