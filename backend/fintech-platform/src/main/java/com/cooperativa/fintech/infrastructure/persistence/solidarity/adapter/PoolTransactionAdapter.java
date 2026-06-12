package com.cooperativa.fintech.infrastructure.persistence.solidarity.adapter;

import com.cooperativa.fintech.domain.solidarity.model.PoolTransaction;
import com.cooperativa.fintech.domain.solidarity.port.PoolTransactionPort;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.mapper.SolidarityPersistenceMapper;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.repository.PoolTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PoolTransactionAdapter implements PoolTransactionPort {

    private final PoolTransactionJpaRepository repository;
    private final SolidarityPersistenceMapper mapper;

    @Override
    public PoolTransaction save(PoolTransaction transaction) {
        return mapper.toDomain(repository.save(mapper.toEntity(transaction)));
    }

    @Override
    public List<PoolTransaction> findByGroupId(UUID groupId) {
        return repository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(mapper::toDomain).toList();
    }
}
