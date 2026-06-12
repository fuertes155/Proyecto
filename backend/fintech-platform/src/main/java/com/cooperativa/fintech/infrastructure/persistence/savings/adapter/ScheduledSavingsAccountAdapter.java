package com.cooperativa.fintech.infrastructure.persistence.savings.adapter;

import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.fintech.domain.savings.port.ScheduledSavingsAccountPort;
import com.cooperativa.fintech.infrastructure.persistence.savings.mapper.ScheduledSavingsPersistenceMapper;
import com.cooperativa.fintech.infrastructure.persistence.savings.repository.ScheduledSavingsAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduledSavingsAccountAdapter implements ScheduledSavingsAccountPort {

    private final ScheduledSavingsAccountJpaRepository repository;
    private final ScheduledSavingsPersistenceMapper mapper;

    @Override
    public ScheduledSavingsAccount save(ScheduledSavingsAccount account) {
        return mapper.toDomain(repository.save(mapper.toEntity(account)));
    }

    @Override
    public Optional<ScheduledSavingsAccount> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ScheduledSavingsAccount> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public List<ScheduledSavingsAccount> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ScheduledSavingsAccount> findDueAccounts(LocalDate date, ScheduledSavingsStatus status) {
        return repository.findByStatusAndNextContributionDateLessThanEqual(status, date).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
