package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.compliance.model.RegulatoryReport;
import com.cooperativa.met.domain.compliance.model.ReportStatus;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.met.domain.compliance.port.RegulatoryReportPort;
import com.cooperativa.met.domain.compliance.port.ReportFileStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadRegulatoryReportUseCaseTest {

    @Mock
    private RegulatoryReportPort reportPort;
    @Mock
    private ReportFileStoragePort storagePort;

    @InjectMocks
    private DownloadRegulatoryReportUseCase downloadUseCase;

    private final UUID reportId = UUID.randomUUID();

    @Test
    void shouldReturnFileWithItsChecksum() {
        when(reportPort.findById(reportId)).thenReturn(Optional.of(report(ReportStatus.COMPLETED, "/reports/a.txt")));
        when(storagePort.read("/reports/a.txt")).thenReturn(new byte[]{1, 2, 3});

        DownloadRegulatoryReportUseCase.DownloadResult result = downloadUseCase.execute(reportId);

        assertEquals("SUPERSOL_COOP001_ASOCIADOS_202609.txt", result.getFileName());
        assertArrayEquals(new byte[]{1, 2, 3}, result.getContent());
        assertEquals("abc123", result.getChecksumSha256());
    }

    @Test
    void shouldRefuseReportThatIsNotFinished() {
        when(reportPort.findById(reportId)).thenReturn(Optional.of(report(ReportStatus.GENERATING, null)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> downloadUseCase.execute(reportId));

        assertEquals("REPORT_NOT_READY", ex.getCode());
        verifyNoInteractions(storagePort);
    }

    @Test
    void shouldFailForUnknownReport() {
        when(reportPort.findById(reportId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> downloadUseCase.execute(reportId));
    }

    private RegulatoryReport report(ReportStatus status, String filePath) {
        return RegulatoryReport.builder()
                .id(reportId)
                .reportType(SupersolidariaReportType.ASOCIADOS)
                .status(status)
                .fileName("SUPERSOL_COOP001_ASOCIADOS_202609.txt")
                .filePath(filePath)
                .checksumSha256("abc123")
                .build();
    }
}
