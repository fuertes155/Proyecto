package com.cooperativa.met.application.notification.usecase;

import com.cooperativa.met.application.notification.dto.NotificationResponse;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetNotificationsUseCase {

    private final NotificationRepositoryPort repository;

    public List<NotificationResponse> execute(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::fromDomain)
                .collect(Collectors.toList());
    }
}
