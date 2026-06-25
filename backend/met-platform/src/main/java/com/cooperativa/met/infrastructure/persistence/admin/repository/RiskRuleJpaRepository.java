package com.cooperativa.met.infrastructure.persistence.admin.repository;

import com.cooperativa.met.infrastructure.persistence.admin.entity.RiskRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskRuleJpaRepository extends JpaRepository<RiskRuleJpaEntity, UUID> {
    List<RiskRuleJpaEntity> findByActivoTrue();
}
