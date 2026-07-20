package com.cooperativa.met.infrastructure.persistence.savings.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.infrastructure.persistence.savings.entity.ScheduledContributionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduledContributionJpaRepository extends JpaRepository<ScheduledContributionJpaEntity, UUID> {

    List<ScheduledContributionJpaEntity> findByAccountIdOrderByScheduledDateDesc(UUID accountId);
}
