package com.cooperativa.met.infrastructure.persistence.admin.adapter;

import com.cooperativa.met.domain.admin.model.RiskRule;
import com.cooperativa.met.domain.admin.port.RiskRuleRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.admin.entity.RiskRuleJpaEntity;
import com.cooperativa.met.infrastructure.persistence.admin.repository.RiskRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskRuleAdapter implements RiskRuleRepositoryPort {

    private final RiskRuleJpaRepository jpaRepository;

    @Override
    public List<RiskRule> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RiskRule> findActivas() {
        return jpaRepository.findByActivoTrue().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<RiskRule> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public RiskRule save(RiskRule rule) {
        return toDomain(jpaRepository.save(toEntity(rule)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private RiskRule toDomain(RiskRuleJpaEntity e) {
        return RiskRule.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .condicion(e.getCondicion())
                .accion(e.getAccion())
                .activo(e.isActivo())
                .creadoPor(e.getCreadoPor())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private RiskRuleJpaEntity toEntity(RiskRule r) {
        RiskRuleJpaEntity e = new RiskRuleJpaEntity();
        e.setId(r.getId() != null ? r.getId() : UUID.randomUUID());
        e.setNombre(r.getNombre());
        e.setDescripcion(r.getDescripcion());
        e.setCondicion(r.getCondicion());
        e.setAccion(r.getAccion());
        e.setActivo(r.isActivo());
        e.setCreadoPor(r.getCreadoPor());
        e.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : Instant.now());
        return e;
    }
}
