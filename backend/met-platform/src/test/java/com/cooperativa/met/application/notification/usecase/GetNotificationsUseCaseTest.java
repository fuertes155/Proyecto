package com.cooperativa.met.application.notification.usecase;

import com.cooperativa.met.application.notification.dto.NotificationResponse;
import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetNotificationsUseCaseTest {

    @Mock
    private NotificationRepositoryPort repository;

    @InjectMocks
    private GetNotificationsUseCase getNotificationsUseCase;

    @InjectMocks
    private GetUnreadNotificationsCountUseCase getUnreadNotificationsCountUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldReturnUserNotificationsNewestFirst() {
        LocalDateTime now = LocalDateTime.now();
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(
                notification("Retiro aprobado", false, now),
                notification("Bienvenido", true, now.minusDays(3))));

        List<NotificationResponse> response = getNotificationsUseCase.execute(userId);

        assertEquals(List.of("Retiro aprobado", "Bienvenido"), response.stream().map(NotificationResponse::getTitle).toList());
        assertFalse(response.get(0).getRead());
        assertTrue(response.get(1).getRead());
        assertEquals(now, response.get(0).getCreatedAt());
    }

    @Test
    void shouldReturnUnreadCount() {
        when(repository.countUnreadByUserId(userId)).thenReturn(4);

        assertEquals(4, getUnreadNotificationsCountUseCase.execute(userId));
    }

    private Notification notification(String title, boolean read, LocalDateTime createdAt) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title(title)
                .message("…")
                .read(read)
                .createdAt(createdAt)
                .build();
    }
}
