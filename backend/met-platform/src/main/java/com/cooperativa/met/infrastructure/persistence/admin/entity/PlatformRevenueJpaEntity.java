package com.cooperativa.met.infrastructure.persistence.admin.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_revenues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformRevenueJpaEntity {
    @Id
    private UUID id;
    private UUID userId;
    private BigDecimal amount;
    private String description;
    private String source;
    private Instant createdAt;
}
