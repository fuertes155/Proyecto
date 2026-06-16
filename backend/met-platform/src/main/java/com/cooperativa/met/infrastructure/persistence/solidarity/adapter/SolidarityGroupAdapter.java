package com.cooperativa.met.infrastructure.persistence.solidarity.adapter;

import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.infrastructure.persistence.solidarity.mapper.SolidarityPersistenceMapper;
import com.cooperativa.met.infrastructure.persistence.solidarity.repository.SolidarityGroupJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SolidarityGroupAdapter implements SolidarityGroupPort {

    private final SolidarityGroupJpaRepository repository;
    private final SolidarityPersistenceMapper mapper;

    @Override
    public SolidarityGroup save(SolidarityGroup group) {
        return mapper.toDomain(repository.save(mapper.toEntity(group)));
    }

    @Override
    public Optional<SolidarityGroup> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SolidarityGroup> findByInviteCode(String inviteCode) {
        return repository.findByInviteCode(inviteCode).map(mapper::toDomain);
    }

    @Override
    public boolean existsByInviteCode(String inviteCode) {
        return repository.existsByInviteCode(inviteCode);
    }

    @Override
    public List<SolidarityGroup> findByUserId(UUID userId) {
        return repository.findByMemberUserId(userId).stream().map(mapper::toDomain).toList();
    }
}
