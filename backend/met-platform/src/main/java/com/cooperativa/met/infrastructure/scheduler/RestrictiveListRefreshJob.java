package com.cooperativa.met.infrastructure.scheduler;

import com.cooperativa.met.application.compliance.usecase.RefreshRestrictiveListsUseCase;
import com.cooperativa.met.application.compliance.usecase.RescreenActiveUsersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantiene el screening SARLAFT vivo sin intervención manual:
 * 1. Descarga las listas OFAC/ONU actualizadas.
 * 2. Vuelve a screenear a todos los usuarios activos contra ellas (una
 *    persona puede entrar a una lista de sanciones DESPUÉS de vincularse).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestrictiveListRefreshJob {

    private final RefreshRestrictiveListsUseCase refreshUseCase;
    private final RescreenActiveUsersUseCase rescreenUseCase;

    /** Todos los días a las 2:00 AM (antes del cierre de reportes de las 00:01 del LoanCollectionJob). */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshAndRescreen() {
        log.info("Iniciando refresco diario de listas restrictivas SARLAFT...");
        refreshUseCase.execute().forEach((listType, result) ->
                log.info("Lista {}: success={} entradas={} error={}",
                        listType, result.success(), result.entriesLoaded(), result.errorMessage()));

        int matches = rescreenUseCase.execute();
        log.info("Refresco y re-screening diario finalizado. {} coincidencias nuevas encontradas.", matches);
    }
}
