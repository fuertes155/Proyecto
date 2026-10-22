package com.cooperativa.met.application.notification.usecase;

import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkNotificationReadUseCaseTest {

    @Mock
    private NotificationRepositoryPort repository;

    @InjectMocks
    private MarkNotificationReadUseCase markNotificationReadUseCase;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();

    @Test
    void shouldMarkOwnNotificationAsRead() {
        when(repository.findById(notificationId)).thenReturn(Optional.of(unread()));

        markNotificationReadUseCase.execute(notificationId, ownerId);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertTrue(saved.getValue().isRead());
    }

    @Test
    void shouldNotLetUserTouchSomeoneElsesNotification() {
        when(repository.findById(notificationId)).thenReturn(Optional.of(unread()));

        assertThrows(IllegalArgumentException.class,
                () -> markNotificationReadUseCase.execute(notificationId, UUID.randomUUID()));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailForUnknownNotification() {
        when(repository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> markNotificationReadUseCase.execute(notificationId, ownerId));

        verify(repository, never()).save(any());
    }

    private Notification unread() {
        return Notification.builder().id(notificationId).userId(ownerId).title("Abono").message("Recibiste $50.000").read(false).build();
    }
}
