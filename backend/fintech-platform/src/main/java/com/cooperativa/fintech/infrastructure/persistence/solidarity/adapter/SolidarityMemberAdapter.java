package com.cooperativa.fintech.infrastructure.persistence.solidarity.adapter;

import com.cooperativa.fintech.domain.solidarity.model.SolidarityMember;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityMemberPort;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.mapper.SolidarityPersistenceMapper;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.repository.SolidarityMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SolidarityMemberAdapter implements SolidarityMemberPort {

    private final SolidarityMemberJpaRepository repository;
    private final SolidarityPersistenceMapper mapper;

    @Override
    public SolidarityMember save(SolidarityMember member) {
        return mapper.toDomain(repository.save(mapper.toEntity(member)));
    }

    @Override
    public Optional<SolidarityMember> findByGroupIdAndUserId(UUID groupId, UUID userId) {
        return repository.findByGroupIdAndUserId(groupId, userId).map(mapper::toDomain);
    }

    @Override
    public List<SolidarityMember> findByGroupId(UUID groupId) {
        return repository.findByGroupIdOrderByJoinedAtAsc(groupId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public int countByGroupId(UUID groupId) {
        return repository.countByGroupId(groupId);
    }
}
