package com.cooperativa.met.infrastructure.scheduler;

import com.cooperativa.met.application.compliance.usecase.GenerateMonthlyReportsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonthlyRegulatoryReportJob {

    private final GenerateMonthlyReportsUseCase generateMonthlyReportsUseCase;

    @Scheduled(cron = "0 0 2 1 * *", zone = "America/Bogota")
    public void generatePreviousMonthReports() {
        generateMonthlyReportsUseCase.execute();
    }
}
