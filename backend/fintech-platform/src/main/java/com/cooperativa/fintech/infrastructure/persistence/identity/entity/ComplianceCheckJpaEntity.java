package com.cooperativa.fintech.infrastructure.persistence.identity.entity;

import com.cooperativa.fintech.domain.identity.model.ComplianceListType;
import com.cooperativa.fintech.domain.identity.model.ComplianceResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_checks")
@Getter
@Setter
public class ComplianceCheckJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 20)
    private ComplianceListType listType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplianceResult result;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private String details;
}
