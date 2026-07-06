package com.cooperativa.met.domain.notification.port;

import com.cooperativa.met.domain.notification.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    int countUnreadByUserId(UUID userId);
    Optional<Notification> findById(UUID id);
    Notification save(Notification notification);
}
