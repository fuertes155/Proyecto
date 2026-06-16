package com.cooperativa.met.domain.savings.port;

import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledSavingsAccountPort {

    ScheduledSavingsAccount save(ScheduledSavingsAccount account);

    Optional<ScheduledSavingsAccount> findById(UUID id);

    Optional<ScheduledSavingsAccount> findByIdAndUserId(UUID id, UUID userId);

    List<ScheduledSavingsAccount> findByUserId(UUID userId);

    List<ScheduledSavingsAccount> findDueAccounts(LocalDate date, ScheduledSavingsStatus status);
}
