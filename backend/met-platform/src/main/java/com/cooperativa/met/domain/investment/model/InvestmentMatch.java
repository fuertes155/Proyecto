package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class InvestmentMatch {
    private final UUID id;
    private final UUID fractionId;
    private final UUID borrowerLoanId;
    private final Instant matchedAt;
}
