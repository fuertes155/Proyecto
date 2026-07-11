package com.cooperativa.met.infrastructure.scheduler;

import com.cooperativa.met.application.investment.usecase.CalculateDailyRoyaltiesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron Job: El reloj de las Regalías
 * Se ejecuta automáticamente todas las noches a las 11:59 PM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestmentRoyaltyJob {

    private final CalculateDailyRoyaltiesUseCase calculateDailyRoyaltiesUseCase;

    @Scheduled(cron = "0 59 23 * * *", zone = "America/Bogota")
    public void processDailyRoyalties() {
        log.info("[CRON] Iniciando El Reloj de las Regalías (11:59 PM)...");
        int procesadas = calculateDailyRoyaltiesUseCase.execute();
        log.info("[CRON] Reloj de las Regalías finalizado. Inversiones actualizadas: {}", procesadas);
    }
}
