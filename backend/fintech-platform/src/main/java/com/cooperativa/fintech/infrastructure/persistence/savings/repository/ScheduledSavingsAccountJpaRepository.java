package com.cooperativa.fintech.infrastructure.persistence.savings.repository;

import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.fintech.infrastructure.persistence.savings.entity.ScheduledSavingsAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledSavingsAccountJpaRepository extends JpaRepository<ScheduledSavingsAccountJpaEntity, UUID> {

    List<ScheduledSavingsAccountJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ScheduledSavingsAccountJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    List<ScheduledSavingsAccountJpaEntity> findByStatusAndNextContributionDateLessThanEqual(
            ScheduledSavingsStatus status,
            LocalDate date
    );
}
