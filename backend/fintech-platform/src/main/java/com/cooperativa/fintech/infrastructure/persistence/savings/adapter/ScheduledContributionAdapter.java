package com.cooperativa.fintech.infrastructure.persistence.savings.adapter;

import com.cooperativa.fintech.domain.savings.model.ScheduledContribution;
import com.cooperativa.fintech.domain.savings.port.ScheduledContributionPort;
import com.cooperativa.fintech.infrastructure.persistence.savings.mapper.ScheduledSavingsPersistenceMapper;
import com.cooperativa.fintech.infrastructure.persistence.savings.repository.ScheduledContributionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduledContributionAdapter implements ScheduledContributionPort {

    private final ScheduledContributionJpaRepository repository;
    private final ScheduledSavingsPersistenceMapper mapper;

    @Override
    public ScheduledContribution save(ScheduledContribution contribution) {
        return mapper.toDomain(repository.save(mapper.toEntity(contribution)));
    }

    @Override
    public List<ScheduledContribution> findByAccountId(UUID accountId) {
        return repository.findByAccountIdOrderByScheduledDateDesc(accountId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
