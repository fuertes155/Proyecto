package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentInstrumentJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.InvestmentInstrumentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvestmentInstrumentAdapter implements InvestmentInstrumentPort {

    private final InvestmentInstrumentJpaRepository jpaRepository;

    @Override
    public List<InvestmentInstrument> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InvestmentInstrument> findActivos() {
        return jpaRepository.findByActivoTrue().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<InvestmentInstrument> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public InvestmentInstrument save(InvestmentInstrument instrument) {
        return toDomain(jpaRepository.save(toEntity(instrument)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private InvestmentInstrument toDomain(InvestmentInstrumentJpaEntity e) {
        return InvestmentInstrument.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .tasaAnual(e.getTasaAnual())
                .plazoDias(e.getPlazoDias())
                .montoMinimo(e.getMontoMinimo())
                .cupoMaximo(e.getCupoMaximo())
                .activo(e.isActivo())
                .creadoPor(e.getCreadoPor())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private InvestmentInstrumentJpaEntity toEntity(InvestmentInstrument i) {
        InvestmentInstrumentJpaEntity e = new InvestmentInstrumentJpaEntity();
        e.setId(i.getId() != null ? i.getId() : UUID.randomUUID());
        e.setNombre(i.getNombre());
        e.setDescripcion(i.getDescripcion());
        e.setTasaAnual(i.getTasaAnual());
        e.setPlazoDias(i.getPlazoDias());
        e.setMontoMinimo(i.getMontoMinimo());
        e.setCupoMaximo(i.getCupoMaximo());
        e.setActivo(i.isActivo());
        e.setCreadoPor(i.getCreadoPor());
        e.setCreatedAt(i.getCreatedAt() != null ? i.getCreatedAt() : Instant.now());
        e.setUpdatedAt(i.getUpdatedAt() != null ? i.getUpdatedAt() : Instant.now());
        return e;
    }
}
