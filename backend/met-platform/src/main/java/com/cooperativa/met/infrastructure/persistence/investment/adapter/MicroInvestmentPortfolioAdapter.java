package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.InvestmentStrategy;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.MicroInvestmentPortfolioJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.MicroInvestmentPortfolioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MicroInvestmentPortfolioAdapter implements MicroInvestmentPortfolioPort {

    private final MicroInvestmentPortfolioJpaRepository jpaRepository;

    @Override
    public MicroInvestmentPortfolio save(MicroInvestmentPortfolio portfolio) {
        return toDomain(jpaRepository.save(toEntity(portfolio)));
    }

    @Override
    public Optional<MicroInvestmentPortfolio> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MicroInvestmentPortfolio> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private MicroInvestmentPortfolio toDomain(MicroInvestmentPortfolioJpaEntity e) {
        return MicroInvestmentPortfolio.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .montoTotal(e.getMontoTotal())
                .estrategia(InvestmentStrategy.valueOf(e.getEstrategia()))
                .estado(InvestmentStatus.valueOf(e.getEstado()))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private MicroInvestmentPortfolioJpaEntity toEntity(MicroInvestmentPortfolio p) {
        MicroInvestmentPortfolioJpaEntity e = new MicroInvestmentPortfolioJpaEntity();
        e.setId(p.getId() != null ? p.getId() : UUID.randomUUID());
        e.setUserId(p.getUserId());
        e.setMontoTotal(p.getMontoTotal());
        e.setEstrategia(p.getEstrategia().name());
        e.setEstado(p.getEstado().name());
        e.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt() : Instant.now());
        e.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : Instant.now());
        return e;
    }
}
