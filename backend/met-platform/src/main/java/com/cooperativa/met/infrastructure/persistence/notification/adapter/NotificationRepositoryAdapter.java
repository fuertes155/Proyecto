package com.cooperativa.met.infrastructure.persistence.notification.adapter;

import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.notification.entity.NotificationJpaEntity;
import com.cooperativa.met.infrastructure.persistence.notification.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository repository;

    @Override
    public List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countUnreadByUserId(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id).map(NotificationJpaEntity::toDomain);
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = NotificationJpaEntity.fromDomain(notification);
        return repository.save(entity).toDomain();
    }
}
