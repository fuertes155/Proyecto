package com.cooperativa.met.infrastructure.scheduler;

import com.cooperativa.met.application.lending.usecase.ProcessLoanCollectionsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron Job: La Cobradora
 * Se ejecuta automáticamente todas las mañanas a las 6:00 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanCollectionJob {

    private final ProcessLoanCollectionsUseCase processLoanCollectionsUseCase;

    @Scheduled(cron = "0 1 0 * * *", zone = "America/Bogota")
    public void processCollections() {
        log.info("[CRON] Iniciando La Cobradora (00:01 AM)...");
        processLoanCollectionsUseCase.execute();
        log.info("[CRON] La Cobradora finalizó sus tareas de gestión de cobro.");
    }
}
