package com.cooperativa.met.application.solidarity.dto;

import com.cooperativa.met.domain.solidarity.model.GroupStatus;
import com.cooperativa.met.domain.solidarity.model.MemberRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SolidarityGroupResponse(
        UUID id,
        String name,
        String description,
        String inviteCode,
        BigDecimal minContribution,
        BigDecimal maxLoanPercentage,
        BigDecimal interestRate,
        BigDecimal poolBalance,
        BigDecimal maxLoanAmount,
        int memberCount,
        int maxMembers,
        GroupStatus status,
        MemberRole myRole,
        Instant createdAt
) {
}
