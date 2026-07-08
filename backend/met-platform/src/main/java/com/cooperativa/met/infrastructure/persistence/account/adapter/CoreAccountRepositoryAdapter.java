package com.cooperativa.met.infrastructure.persistence.account.adapter;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.account.entity.CoreAccountJpaEntity;
import com.cooperativa.met.infrastructure.persistence.account.repository.CoreAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CoreAccountRepositoryAdapter implements CoreAccountRepositoryPort {

    private final CoreAccountJpaRepository repository;

    @Override
    public Optional<CoreAccount> findById(UUID id) {
        return repository.findById(id).map(CoreAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<CoreAccount> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(CoreAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<CoreAccount> findByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber).map(CoreAccountJpaEntity::toDomain);
    }

    @Override
    public CoreAccount save(CoreAccount account) {
        CoreAccountJpaEntity entity = CoreAccountJpaEntity.fromDomain(account);
        return repository.save(entity).toDomain();
    }

    @Override
    public java.util.List<CoreAccount> findAll() {
        return repository.findAll().stream().map(CoreAccountJpaEntity::toDomain).toList();
    }
}
