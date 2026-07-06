package com.cooperativa.met.infrastructure.persistence.notification.entity;

import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.infrastructure.persistence.identity.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(nullable = false, length = 150)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "is_read", nullable = false)
    private boolean read;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Notification toDomain() {
        return Notification.builder()
                .id(this.id)
                .userId(this.userId)
                .title(this.title)
                .message(this.message)
                .read(this.read)
                .createdAt(this.createdAt)
                .build();
    }

    public static NotificationJpaEntity fromDomain(Notification domain) {
        return NotificationJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .title(domain.getTitle())
                .message(domain.getMessage())
                .read(domain.isRead())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
