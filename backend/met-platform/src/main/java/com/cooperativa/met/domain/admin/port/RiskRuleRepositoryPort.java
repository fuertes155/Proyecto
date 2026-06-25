package com.cooperativa.met.domain.admin.port;

import com.cooperativa.met.domain.admin.model.RiskRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskRuleRepositoryPort {
    List<RiskRule> findAll();
    List<RiskRule> findActivas();
    Optional<RiskRule> findById(UUID id);
    RiskRule save(RiskRule rule);
    void deleteById(UUID id);
}
