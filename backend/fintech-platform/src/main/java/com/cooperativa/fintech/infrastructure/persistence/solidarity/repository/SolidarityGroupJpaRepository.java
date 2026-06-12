package com.cooperativa.fintech.infrastructure.persistence.solidarity.repository;

import com.cooperativa.fintech.infrastructure.persistence.solidarity.entity.SolidarityGroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolidarityGroupJpaRepository extends JpaRepository<SolidarityGroupJpaEntity, UUID> {

    Optional<SolidarityGroupJpaEntity> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    @Query("""
            SELECT g FROM SolidarityGroupJpaEntity g
            JOIN SolidarityMemberJpaEntity m ON m.groupId = g.id
            WHERE m.userId = :userId
            ORDER BY g.createdAt DESC
            """)
    List<SolidarityGroupJpaEntity> findByMemberUserId(@Param("userId") UUID userId);
}
