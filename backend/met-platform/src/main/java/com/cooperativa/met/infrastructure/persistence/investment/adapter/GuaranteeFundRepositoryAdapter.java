package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.GuaranteeFundMovement;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovementType;
import com.cooperativa.met.domain.investment.port.GuaranteeFundPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.GuaranteeFundMovementJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.GuaranteeFundMovementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GuaranteeFundRepositoryAdapter implements GuaranteeFundPort {

    private final GuaranteeFundMovementJpaRepository repository;

    @Override
    public GuaranteeFundMovement save(GuaranteeFundMovement movement) {
        return toModel(repository.save(toEntity(movement)));
    }

    @Override
    public BigDecimal getBalance() {
        return repository.calculateBalance();
    }

    @Override
    public List<GuaranteeFundMovement> findByTransactionReference(UUID transactionReference) {
        return repository.findByTransactionReference(transactionReference).stream().map(this::toModel).toList();
    }

    private GuaranteeFundMovementJpaEntity toEntity(GuaranteeFundMovement model) {
        return GuaranteeFundMovementJpaEntity.builder()
                .id(model.getId())
                .type(model.getType().name())
                .amount(model.getAmount())
                .transactionReference(model.getTransactionReference())
                .concept(model.getConcept())
                .createdAt(model.getCreatedAt())
                .build();
    }

    private GuaranteeFundMovement toModel(GuaranteeFundMovementJpaEntity entity) {
        return GuaranteeFundMovement.builder()
                .id(entity.getId())
                .type(GuaranteeFundMovementType.valueOf(entity.getType()))
                .amount(entity.getAmount())
                .transactionReference(entity.getTransactionReference())
                .concept(entity.getConcept())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
