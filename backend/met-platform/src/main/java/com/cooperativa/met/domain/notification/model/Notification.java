package com.cooperativa.met.domain.notification.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Notification {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
