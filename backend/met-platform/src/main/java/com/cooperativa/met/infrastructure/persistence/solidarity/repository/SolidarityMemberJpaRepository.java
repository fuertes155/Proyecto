package com.cooperativa.met.infrastructure.persistence.solidarity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.infrastructure.persistence.solidarity.entity.SolidarityMemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolidarityMemberJpaRepository extends JpaRepository<SolidarityMemberJpaEntity, UUID> {

    Optional<SolidarityMemberJpaEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);

    List<SolidarityMemberJpaEntity> findByGroupIdOrderByJoinedAtAsc(UUID groupId);

    int countByGroupId(UUID groupId);
}
