package com.cooperativa.met.application.solidarity.dto;

import com.cooperativa.met.domain.solidarity.model.MemberRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SolidarityMemberResponse(
        UUID id,
        UUID userId,
        MemberRole role,
        BigDecimal totalContributed,
        Instant joinedAt
) {
}
