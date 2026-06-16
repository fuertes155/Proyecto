package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.GenerateReportRequest;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateMonthlyReportsUseCase {

    private final GenerateSupersolidariaReportUseCase generateReportUseCase;

    public int execute() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        int year = previousMonth.getYear();
        int month = previousMonth.getMonthValue();
        int generated = 0;

        for (SupersolidariaReportType type : SupersolidariaReportType.values()) {
            try {
                generateReportUseCase.execute(null,
                        new GenerateReportRequest(type, year, month));
                generated++;
            } catch (com.cooperativa.met.domain.common.exception.BusinessRuleException ex) {
                if ("REPORT_ALREADY_EXISTS".equals(ex.getCode())) {
                    log.debug("Reporte {} para {}/{} ya existe", type, year, month);
                } else {
                    log.warn("No se pudo generar reporte {} para {}/{}: {}", type, year, month, ex.getMessage());
                }
            } catch (Exception ex) {
                log.warn("No se pudo generar reporte {} para {}/{}: {}", type, year, month, ex.getMessage());
            }
        }
        return generated;
    }
}
