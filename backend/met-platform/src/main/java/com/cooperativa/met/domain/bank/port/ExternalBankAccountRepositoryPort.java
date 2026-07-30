package com.cooperativa.met.domain.bank.port;

import com.cooperativa.met.domain.bank.model.ExternalBankAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalBankAccountRepositoryPort {
    Optional<ExternalBankAccount> findById(UUID id);
    List<ExternalBankAccount> findActiveByUserId(UUID userId);
    ExternalBankAccount save(ExternalBankAccount account);
}
