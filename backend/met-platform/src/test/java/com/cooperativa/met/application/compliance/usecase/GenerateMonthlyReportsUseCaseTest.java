package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.GenerateReportRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GenerateMonthlyReportsUseCaseTest {

    private static final int REPORT_TYPES = SupersolidariaReportType.values().length;

    @Mock
    private GenerateSupersolidariaReportUseCase generateReportUseCase;

    @InjectMocks
    private GenerateMonthlyReportsUseCase generateMonthlyReportsUseCase;

    @Test
    void shouldGenerateEveryReportTypeForThePreviousMonth() {
        int generated = generateMonthlyReportsUseCase.execute();

        assertEquals(REPORT_TYPES, generated);
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        ArgumentCaptor<GenerateReportRequest> requests = ArgumentCaptor.forClass(GenerateReportRequest.class);
        verify(generateReportUseCase, times(REPORT_TYPES)).execute(isNull(), requests.capture());
        assertTrue(requests.getAllValues().stream().allMatch(r ->
                r.periodYear() == previousMonth.getYear() && r.periodMonth() == previousMonth.getMonthValue()));
        assertEquals(Arrays.asList(SupersolidariaReportType.values()),
                requests.getAllValues().stream().map(GenerateReportRequest::reportType).toList());
    }

    @Test
    void shouldKeepGoingWhenOneReportAlreadyExistsOrFails() {
        // lenient: los demás tipos de reporte llaman al mismo método con otros argumentos a propósito.
        lenient().when(generateReportUseCase.execute(isNull(), argThat(r -> r != null && r.reportType() == SupersolidariaReportType.ASOCIADOS)))
                .thenThrow(new BusinessRuleException("REPORT_ALREADY_EXISTS", "ya existe"));
        lenient().when(generateReportUseCase.execute(isNull(), argThat(r -> r != null && r.reportType() == SupersolidariaReportType.SARLAFT)))
                .thenThrow(new IllegalStateException("BD caída"));

        int generated = generateMonthlyReportsUseCase.execute();

        assertEquals(REPORT_TYPES - 2, generated);
        verify(generateReportUseCase, times(REPORT_TYPES)).execute(isNull(), any());
    }
}
