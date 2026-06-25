package com.cooperativa.met.infrastructure.persistence.admin.adapter;

import com.cooperativa.met.domain.admin.model.OperationLimit;
import com.cooperativa.met.domain.admin.port.OperationLimitRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.admin.entity.OperationLimitJpaEntity;
import com.cooperativa.met.infrastructure.persistence.admin.repository.OperationLimitJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OperationLimitAdapter implements OperationLimitRepositoryPort {

    private final OperationLimitJpaRepository jpaRepository;

    @Override
    public List<OperationLimit> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<OperationLimit> findByTipo(String tipoOperacion) {
        return jpaRepository.findByTipoOperacion(tipoOperacion).map(this::toDomain);
    }

    @Override
    public OperationLimit save(OperationLimit limit) {
        return toDomain(jpaRepository.save(toEntity(limit)));
    }

    private OperationLimit toDomain(OperationLimitJpaEntity e) {
        return OperationLimit.builder()
                .id(e.getId())
                .tipoOperacion(e.getTipoOperacion())
                .montoDiarioMax(e.getMontoDiarioMax())
                .montoPorTransaccionMax(e.getMontoPorTransaccionMax())
                .activo(e.isActivo())
                .creadoPor(e.getCreadoPor())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private OperationLimitJpaEntity toEntity(OperationLimit l) {
        OperationLimitJpaEntity e = new OperationLimitJpaEntity();
        e.setId(l.getId() != null ? l.getId() : UUID.randomUUID());
        e.setTipoOperacion(l.getTipoOperacion());
        e.setMontoDiarioMax(l.getMontoDiarioMax());
        e.setMontoPorTransaccionMax(l.getMontoPorTransaccionMax());
        e.setActivo(l.isActivo());
        e.setCreadoPor(l.getCreadoPor());
        e.setUpdatedAt(l.getUpdatedAt() != null ? l.getUpdatedAt() : Instant.now());
        return e;
    }
}
