package com.cooperativa.fintech.infrastructure.scheduler;

import com.cooperativa.fintech.application.savings.usecase.ProcessDueContributionsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledContributionJob {

    private final ProcessDueContributionsUseCase processDueContributionsUseCase;

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Bogota")
    public void processDailyContributions() {
        processDueContributionsUseCase.execute();
    }
}
