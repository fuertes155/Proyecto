package com.cooperativa.met.infrastructure.scheduler;

import com.cooperativa.met.application.lending.usecase.ReportCurrentLoansToCreditBureauUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron Job: Reporte mensual de vida crediticia ("al día") a la central de riesgo.
 * Se ejecuta el primer día de cada mes, después de "La Cobradora" y del reporte
 * regulatorio mensual, para evitar contención de recursos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditBureauMonthlyReportJob {

    private final ReportCurrentLoansToCreditBureauUseCase reportCurrentLoansToCreditBureauUseCase;

    @Scheduled(cron = "0 0 4 1 * *", zone = "America/Bogota")
    public void reportCurrentLoans() {
        log.info("[CRON] Iniciando reporte mensual de vida crediticia (00:04 AM, día 1)...");
        reportCurrentLoansToCreditBureauUseCase.execute();
    }
}
