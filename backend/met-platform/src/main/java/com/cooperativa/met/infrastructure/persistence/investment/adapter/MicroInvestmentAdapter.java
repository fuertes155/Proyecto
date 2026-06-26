package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.MicroInvestmentJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.MicroInvestmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MicroInvestmentAdapter implements MicroInvestmentPort {

    private final MicroInvestmentJpaRepository jpaRepository;

    @Override
    public MicroInvestment save(MicroInvestment investment) {
        return toDomain(jpaRepository.save(toEntity(investment)));
    }

    @Override
    public Optional<MicroInvestment> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MicroInvestment> findByPortfolioId(UUID portfolioId) {
        return jpaRepository.findByPortfolioId(portfolioId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MicroInvestment> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MicroInvestment> findMaturingOnOrBefore(LocalDate date, InvestmentStatus status) {
        return jpaRepository.findMaturingOnOrBefore(date, status.name()).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    private MicroInvestment toDomain(MicroInvestmentJpaEntity e) {
        return MicroInvestment.builder()
                .id(e.getId())
                .portfolioId(e.getPortfolioId())
                .instrumentId(e.getInstrumentId())
                .userId(e.getUserId())
                .montoInvertido(e.getMontoInvertido())
                .tasaAplicada(e.getTasaAplicada())
                .plazoDias(e.getPlazoDias())
                .fechaInicio(e.getFechaInicio())
                .fechaVencimiento(e.getFechaVencimiento())
                .rendimientoGanado(e.getRendimientoGanado())
                .estado(InvestmentStatus.valueOf(e.getEstado()))
                .canceladoAt(e.getCanceladoAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private MicroInvestmentJpaEntity toEntity(MicroInvestment inv) {
        MicroInvestmentJpaEntity e = new MicroInvestmentJpaEntity();
        e.setId(inv.getId() != null ? inv.getId() : UUID.randomUUID());
        e.setPortfolioId(inv.getPortfolioId());
        e.setInstrumentId(inv.getInstrumentId());
        e.setUserId(inv.getUserId());
        e.setMontoInvertido(inv.getMontoInvertido());
        e.setTasaAplicada(inv.getTasaAplicada());
        e.setPlazoDias(inv.getPlazoDias());
        e.setFechaInicio(inv.getFechaInicio() != null ? inv.getFechaInicio() : LocalDate.now());
        e.setFechaVencimiento(inv.getFechaVencimiento());
        e.setRendimientoGanado(inv.getRendimientoGanado());
        e.setEstado(inv.getEstado().name());
        e.setCanceladoAt(inv.getCanceladoAt());
        e.setCreatedAt(inv.getCreatedAt() != null ? inv.getCreatedAt() : Instant.now());
        return e;
    }
}
