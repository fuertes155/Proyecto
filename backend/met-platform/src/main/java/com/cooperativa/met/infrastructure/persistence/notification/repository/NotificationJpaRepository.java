package com.cooperativa.met.infrastructure.persistence.notification.repository;

import com.cooperativa.met.infrastructure.persistence.notification.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    int countByUserIdAndReadFalse(UUID userId);
}
