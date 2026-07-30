package com.cooperativa.met.infrastructure.persistence.bank.adapter;

import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.bank.entity.BankJpaEntity;
import com.cooperativa.met.infrastructure.persistence.bank.repository.BankJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BankRepositoryAdapter implements BankRepositoryPort {

    private final BankJpaRepository repository;

    @Override
    public List<Bank> findAllActive() {
        return repository.findByActiveTrue().stream().map(BankJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Bank> findByCode(String code) {
        return repository.findById(code).map(BankJpaEntity::toDomain);
    }

    @Override
    public Bank save(Bank bank) {
        return repository.save(BankJpaEntity.fromDomain(bank)).toDomain();
    }
}
