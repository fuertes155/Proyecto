package com.cooperativa.met.infrastructure.persistence.webhook.repository;

import com.cooperativa.met.infrastructure.persistence.webhook.entity.ProcessedWebhookJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedWebhookJpaRepository extends JpaRepository<ProcessedWebhookJpaEntity, String> {
}
