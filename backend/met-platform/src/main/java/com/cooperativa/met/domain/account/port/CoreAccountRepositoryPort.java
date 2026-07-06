package com.cooperativa.met.domain.account.port;

import com.cooperativa.met.domain.account.model.CoreAccount;

import java.util.Optional;
import java.util.UUID;

public interface CoreAccountRepositoryPort {
    Optional<CoreAccount> findById(UUID id);
    Optional<CoreAccount> findByUserId(UUID userId);
    Optional<CoreAccount> findByAccountNumber(String accountNumber);
    CoreAccount save(CoreAccount account);
}
