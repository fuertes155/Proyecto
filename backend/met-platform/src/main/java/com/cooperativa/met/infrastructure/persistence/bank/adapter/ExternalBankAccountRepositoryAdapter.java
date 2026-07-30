package com.cooperativa.met.infrastructure.persistence.bank.adapter;

import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.bank.entity.ExternalBankAccountJpaEntity;
import com.cooperativa.met.infrastructure.persistence.bank.repository.ExternalBankAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalBankAccountRepositoryAdapter implements ExternalBankAccountRepositoryPort {

    private final ExternalBankAccountJpaRepository repository;

    @Override
    public Optional<ExternalBankAccount> findById(UUID id) {
        return repository.findById(id).map(ExternalBankAccountJpaEntity::toDomain);
    }

    @Override
    public List<ExternalBankAccount> findActiveByUserId(UUID userId) {
        return repository.findByUserIdAndActiveTrue(userId).stream()
                .map(ExternalBankAccountJpaEntity::toDomain)
                .toList();
    }

    @Override
    public ExternalBankAccount save(ExternalBankAccount account) {
        return repository.save(ExternalBankAccountJpaEntity.fromDomain(account)).toDomain();
    }
}
