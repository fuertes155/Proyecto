package com.cooperativa.met.application.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HomeSummaryResponse(
        UUID userId,
        BigDecimal principalBalance,
        BigDecimal interestBalance,
        String accountNumber,
        String accountStatus,
        List<RecentTransactionResponse> recentTransactions,
        BigDecimal savingsTotal,
        BigDecimal investmentsTotal,
        int pendingLoans
) {
    public record RecentTransactionResponse(
            UUID id,
            String type,
            BigDecimal amount,
            String description,
            Instant createdAt,
            boolean isCredit
    ) {
    }
}
