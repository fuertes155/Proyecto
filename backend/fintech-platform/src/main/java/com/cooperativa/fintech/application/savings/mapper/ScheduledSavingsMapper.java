package com.cooperativa.fintech.application.savings.mapper;

import com.cooperativa.fintech.application.savings.dto.ContributionResponse;
import com.cooperativa.fintech.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.fintech.domain.savings.model.ScheduledContribution;
import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ScheduledSavingsMapper {

    public ScheduledSavingsResponse toResponse(ScheduledSavingsAccount account) {
        return new ScheduledSavingsResponse(
                account.getId(),
                account.getName(),
                account.getTargetAmount(),
                account.getContributionAmount(),
                account.getFrequency(),
                account.getDebitDayOfWeek(),
                account.getDebitDayOfMonth(),
                account.getCurrentBalance(),
                calculateProgress(account),
                account.getStatus(),
                account.getNextContributionDate(),
                account.getCreatedAt()
        );
    }

    public ContributionResponse toResponse(ScheduledContribution contribution) {
        return new ContributionResponse(
                contribution.getId(),
                contribution.getAccountId(),
                contribution.getAmount(),
                contribution.getScheduledDate(),
                contribution.getExecutedAt(),
                contribution.getStatus(),
                contribution.getFailureReason()
        );
    }

    private BigDecimal calculateProgress(ScheduledSavingsAccount account) {
        if (account.getTargetAmount() == null || account.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return account.getCurrentBalance()
                .multiply(BigDecimal.valueOf(100))
                .divide(account.getTargetAmount(), 2, RoundingMode.HALF_UP);
    }
}
