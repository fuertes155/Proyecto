package com.cooperativa.fintech.infrastructure.persistence.savings.repository;

import com.cooperativa.fintech.infrastructure.persistence.savings.entity.ScheduledContributionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduledContributionJpaRepository extends JpaRepository<ScheduledContributionJpaEntity, UUID> {

    List<ScheduledContributionJpaEntity> findByAccountIdOrderByScheduledDateDesc(UUID accountId);
}
