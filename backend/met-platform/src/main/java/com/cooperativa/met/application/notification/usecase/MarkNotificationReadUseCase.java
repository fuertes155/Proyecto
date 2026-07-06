package com.cooperativa.met.application.notification.usecase;

import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkNotificationReadUseCase {

    private final NotificationRepositoryPort repository;

    @Transactional
    public void execute(UUID notificationId, UUID userId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("No tiene permisos para modificar esta notificación");
        }

        notification.setRead(true);
        repository.save(notification);
    }
}
