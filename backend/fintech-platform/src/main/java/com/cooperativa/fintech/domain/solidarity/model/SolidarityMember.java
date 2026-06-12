package com.cooperativa.fintech.domain.solidarity.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class SolidarityMember {

    private final UUID id;
    private final UUID groupId;
    private final UUID userId;
    private final MemberRole role;
    private final BigDecimal totalContributed;
    private final Instant joinedAt;

    public SolidarityMember addContribution(BigDecimal amount) {
        return toBuilder()
                .totalContributed(totalContributed.add(amount))
                .build();
    }
}
