package com.cooperativa.met.infrastructure.web.notification;

import com.cooperativa.met.application.notification.dto.NotificationResponse;
import com.cooperativa.met.application.notification.usecase.GetNotificationsUseCase;
import com.cooperativa.met.application.notification.usecase.GetUnreadNotificationsCountUseCase;
import com.cooperativa.met.application.notification.usecase.MarkNotificationReadUseCase;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final GetUnreadNotificationsCountUseCase getUnreadNotificationsCountUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken jwtToken) {
            return (UUID) jwtToken.getPrincipal();
        }
        throw new IllegalStateException("No se pudo obtener el ID del usuario autenticado");
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications() {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(getNotificationsUseCase.execute(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount() {
        UUID userId = getAuthenticatedUserId();
        int count = getUnreadNotificationsCountUseCase.execute(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        UUID userId = getAuthenticatedUserId();
        markNotificationReadUseCase.execute(id, userId);
        return ResponseEntity.noContent().build();
    }
}
