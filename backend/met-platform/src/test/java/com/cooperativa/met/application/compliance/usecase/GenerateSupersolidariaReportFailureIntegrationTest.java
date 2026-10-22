package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.GenerateReportRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.compliance.model.RegulatoryReport;
import com.cooperativa.met.domain.compliance.model.ReportStatus;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.met.domain.compliance.port.RegulatoryReportPort;
import com.cooperativa.met.domain.compliance.port.ReportDatasetPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Con base de datos real (H2): el caso de uso guarda el reporte como FAILED y enseguida lanza la
 * excepción. Lo que se prueba es que ese FAILED quede guardado (para verlo en el panel y poder
 * reintentar), cosa que un test con mocks no puede ver.
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class GenerateSupersolidariaReportFailureIntegrationTest {

    @Autowired
    private GenerateSupersolidariaReportUseCase generateReportUseCase;
    @Autowired
    private RegulatoryReportPort reportPort;
    @MockBean
    private ReportDatasetPort datasetPort;

    @Test
    void shouldLeaveTheFailedAttemptRecorded() {
        when(datasetPort.buildDataset(any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new IllegalStateException("consulta de asociados falló"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> generateReportUseCase.execute(null,
                new GenerateReportRequest(SupersolidariaReportType.SARLAFT, 2025, 3)));

        assertEquals("REPORT_GENERATION_FAILED", ex.getCode());
        RegulatoryReport stored = reportPort.findByTypeAndPeriod(SupersolidariaReportType.SARLAFT, 2025, 3)
                .orElseThrow(() -> new AssertionError("el intento fallido no quedó registrado"));
        assertEquals(ReportStatus.FAILED, stored.getStatus());
        assertEquals("consulta de asociados falló", stored.getErrorMessage());
    }
}
