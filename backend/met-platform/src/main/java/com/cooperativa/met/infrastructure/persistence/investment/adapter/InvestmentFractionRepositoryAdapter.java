package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentFractionJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.InvestmentFractionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvestmentFractionRepositoryAdapter implements InvestmentFractionRepositoryPort {

    private final InvestmentFractionJpaRepository repository;

    @Override
    public InvestmentFraction save(InvestmentFraction fraction) {
        return toModel(repository.save(toEntity(fraction)));
    }

    @Override
    public List<InvestmentFraction> saveAll(List<InvestmentFraction> fractions) {
        List<InvestmentFractionJpaEntity> entities = fractions.stream().map(this::toEntity).toList();
        return repository.saveAll(entities).stream().map(this::toModel).toList();
    }

    @Override
    public Optional<InvestmentFraction> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public List<InvestmentFraction> findByStatus(InvestmentFractionStatus status) {
        return repository.findByStatus(status.name()).stream().map(this::toModel).toList();
    }

    @Override
    public List<InvestmentFraction> findByOriginalDepositId(UUID depositId) {
        return repository.findByOriginalDepositId(depositId).stream().map(this::toModel).toList();
    }

    private InvestmentFractionJpaEntity toEntity(InvestmentFraction model) {
        return InvestmentFractionJpaEntity.builder()
                .id(model.getId())
                .investorAccountId(model.getInvestorAccountId())
                .originalDepositId(model.getOriginalDepositId())
                .amount(model.getAmount())
                .status(model.getStatus().name())
                .createdAt(model.getCreatedAt())
                .matchedAt(model.getMatchedAt())
                .build();
    }

    private InvestmentFraction toModel(InvestmentFractionJpaEntity entity) {
        return InvestmentFraction.builder()
                .id(entity.getId())
                .investorAccountId(entity.getInvestorAccountId())
                .originalDepositId(entity.getOriginalDepositId())
                .amount(entity.getAmount())
                .status(InvestmentFractionStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .matchedAt(entity.getMatchedAt())
                .build();
    }
}
