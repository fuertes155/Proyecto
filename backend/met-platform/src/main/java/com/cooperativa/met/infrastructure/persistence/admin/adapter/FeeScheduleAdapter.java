package com.cooperativa.met.infrastructure.persistence.admin.adapter;

import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.admin.entity.FeeScheduleJpaEntity;
import com.cooperativa.met.infrastructure.persistence.admin.repository.FeeScheduleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeeScheduleAdapter implements FeeScheduleRepositoryPort {

    private final FeeScheduleJpaRepository jpaRepository;

    @Override
    public List<FeeSchedule> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<FeeSchedule> findVigentes() {
        return jpaRepository.findVigentes(Instant.now()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<FeeSchedule> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public FeeSchedule save(FeeSchedule fee) {
        return toDomain(jpaRepository.save(toEntity(fee)));
    }

    @Override
    @Transactional
    public void cerrarVigencia(String tipoTarifa) {
        jpaRepository.cerrarVigencia(tipoTarifa, Instant.now());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }


    private FeeSchedule toDomain(FeeScheduleJpaEntity e) {
        return FeeSchedule.builder()
                .id(e.getId())
                .tipoTarifa(e.getTipoTarifa())
                .descripcion(e.getDescripcion())
                .valor(e.getValor())
                .esPorcentaje(e.isEsPorcentaje())
                .vigentDesde(e.getVigentDesde())
                .vigentaHasta(e.getVigentaHasta())
                .creadoPor(e.getCreadoPor())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private FeeScheduleJpaEntity toEntity(FeeSchedule f) {
        FeeScheduleJpaEntity e = new FeeScheduleJpaEntity();
        e.setId(f.getId() != null ? f.getId() : UUID.randomUUID());
        e.setTipoTarifa(f.getTipoTarifa());
        e.setDescripcion(f.getDescripcion());
        e.setValor(f.getValor());
        e.setEsPorcentaje(f.isEsPorcentaje());
        e.setVigentDesde(f.getVigentDesde());
        e.setVigentaHasta(f.getVigentaHasta());
        e.setCreadoPor(f.getCreadoPor());
        e.setCreatedAt(f.getCreatedAt() != null ? f.getCreatedAt() : Instant.now());
        return e;
    }
}
