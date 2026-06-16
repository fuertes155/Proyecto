package com.cooperativa.met.infrastructure.persistence.solidarity.entity;

import com.cooperativa.met.domain.solidarity.model.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solidarity_members")
@Getter
@Setter
public class SolidarityMemberJpaEntity {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "total_contributed", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalContributed;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
}
