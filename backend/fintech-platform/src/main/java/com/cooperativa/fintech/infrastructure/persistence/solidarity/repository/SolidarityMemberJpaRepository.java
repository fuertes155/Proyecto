package com.cooperativa.fintech.infrastructure.persistence.solidarity.repository;

import com.cooperativa.fintech.infrastructure.persistence.solidarity.entity.SolidarityMemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolidarityMemberJpaRepository extends JpaRepository<SolidarityMemberJpaEntity, UUID> {

    Optional<SolidarityMemberJpaEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);

    List<SolidarityMemberJpaEntity> findByGroupIdOrderByJoinedAtAsc(UUID groupId);

    int countByGroupId(UUID groupId);
}
