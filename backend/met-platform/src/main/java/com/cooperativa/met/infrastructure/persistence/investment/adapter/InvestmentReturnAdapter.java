package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentReturnJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.InvestmentReturnJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvestmentReturnAdapter implements InvestmentReturnPort {

    private final InvestmentReturnJpaRepository jpaRepository;

    @Override
    public InvestmentReturn save(InvestmentReturn investmentReturn) {
        return toDomain(jpaRepository.save(toEntity(investmentReturn)));
    }

    @Override
    public List<InvestmentReturn> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByFechaPagoDesc(userId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    private InvestmentReturn toDomain(InvestmentReturnJpaEntity e) {
        return InvestmentReturn.builder()
                .id(e.getId())
                .investmentId(e.getInvestmentId())
                .userId(e.getUserId())
                .capital(e.getCapital())
                .rendimiento(e.getRendimiento())
                .totalAcreditado(e.getTotalAcreditado())
                .fechaPago(e.getFechaPago())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private InvestmentReturnJpaEntity toEntity(InvestmentReturn r) {
        InvestmentReturnJpaEntity e = new InvestmentReturnJpaEntity();
        e.setId(r.getId() != null ? r.getId() : UUID.randomUUID());
        e.setInvestmentId(r.getInvestmentId());
        e.setUserId(r.getUserId());
        e.setCapital(r.getCapital());
        e.setRendimiento(r.getRendimiento());
        e.setTotalAcreditado(r.getTotalAcreditado());
        e.setFechaPago(r.getFechaPago());
        e.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : Instant.now());
        return e;
    }
}
