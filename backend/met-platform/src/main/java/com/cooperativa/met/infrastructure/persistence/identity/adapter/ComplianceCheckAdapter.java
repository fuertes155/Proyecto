package com.cooperativa.met.infrastructure.persistence.identity.adapter;

import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.infrastructure.persistence.identity.entity.ComplianceCheckJpaEntity;
import com.cooperativa.met.infrastructure.persistence.identity.repository.ComplianceCheckJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ComplianceCheckAdapter implements ComplianceCheckPort {

    private final ComplianceCheckJpaRepository repository;

    @Override
    public ComplianceResult checkUser(UUID userId, ComplianceListType listType) {
        // Integración futura con proveedor SARLAFT/OFAC/ONU
        return ComplianceResult.CLEAR;
    }

    @Override
    public void persistCheck(UUID userId, ComplianceListType listType, ComplianceResult result, String details) {
        ComplianceCheckJpaEntity entity = new ComplianceCheckJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setListType(listType);
        entity.setResult(result);
        entity.setCheckedAt(Instant.now());
        entity.setDetails(details);
        repository.save(entity);
    }
}
