package com.cooperativa.met.infrastructure.scheduler;

import com.cooperativa.met.application.investment.usecase.ProcessMaturingInvestmentsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job diario que procesa las posiciones de micro-inversión vencidas.
 * Se ejecuta a las 07:00 (Bogotá), una hora después del job de aportes,
 * para evitar solapamiento de transacciones en el saldo del usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestmentMaturationJob {

    private final ProcessMaturingInvestmentsUseCase processMaturingInvestmentsUseCase;

    @Scheduled(cron = "0 0 7 * * *", zone = "America/Bogota")
    public void processDailyMaturations() {
        log.info("Iniciando procesamiento de inversiones vencidas...");
        int procesadas = processMaturingInvestmentsUseCase.execute();
        log.info("Procesamiento finalizado. Total inversiones maduradas: {}", procesadas);
    }
}
