package com.cooperativa.met.domain.identity.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/** Un registro histórico de screening (ver {@code compliance_checks}). */
@Getter
@Builder
public class ComplianceCheckRecord {
    private final UUID id;
    private final UUID userId;
    private final ComplianceListType listType;
    private final ComplianceResult result;
    private final Instant checkedAt;
    private final String details;
}
