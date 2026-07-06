package com.cooperativa.met.application.notification.usecase;

import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUnreadNotificationsCountUseCase {

    private final NotificationRepositoryPort repository;

    public int execute(UUID userId) {
        return repository.countUnreadByUserId(userId);
    }
}
