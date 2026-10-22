package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.GenerateReportRequest;
import com.cooperativa.met.application.compliance.dto.RegulatoryReportResponse;
import com.cooperativa.met.application.compliance.mapper.RegulatoryReportMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.compliance.model.RegulatoryReport;
import com.cooperativa.met.domain.compliance.model.ReportDataRow;
import com.cooperativa.met.domain.compliance.model.ReportDataset;
import com.cooperativa.met.domain.compliance.model.ReportStatus;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.met.domain.compliance.port.RegulatoryReportPort;
import com.cooperativa.met.domain.compliance.port.ReportDatasetPort;
import com.cooperativa.met.domain.compliance.port.ReportFileStoragePort;
import com.cooperativa.met.infrastructure.config.MetComplianceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateSupersolidariaReportUseCaseTest {

    private static final GenerateReportRequest REQUEST = new GenerateReportRequest(SupersolidariaReportType.ASOCIADOS, 2026, 9);

    @Mock
    private RegulatoryReportPort reportPort;
    @Mock
    private ReportDatasetPort datasetPort;
    @Mock
    private ReportFileStoragePort storagePort;

    private GenerateSupersolidariaReportUseCase generateReportUseCase;

    private final UUID officerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        generateReportUseCase = new GenerateSupersolidariaReportUseCase(reportPort, datasetPort, storagePort,
                new MetComplianceProperties(), new RegulatoryReportMapper());
    }

    @Test
    void shouldGenerateStoreAndFingerprintTheFlatFile() throws Exception {
        when(reportPort.findByTypeAndPeriod(SupersolidariaReportType.ASOCIADOS, 2026, 9)).thenReturn(Optional.empty());
        when(reportPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(datasetPort.buildDataset(SupersolidariaReportType.ASOCIADOS, 2026, 9, "COOP001")).thenReturn(dataset());
        when(storagePort.store(eq("SUPERSOL_COOP001_ASOCIADOS_202609.txt"), any())).thenReturn("/reports/202609/asociados.txt");

        RegulatoryReportResponse response = generateReportUseCase.execute(officerId, REQUEST);

        ArgumentCaptor<byte[]> stored = ArgumentCaptor.forClass(byte[].class);
        verify(storagePort).store(anyString(), stored.capture());
        String content = new String(stored.getValue(), StandardCharsets.UTF_8);
        assertTrue(content.contains("D|52123456|Ana Pérez"), content);
        assertTrue(content.contains("T|2"), content);

        assertEquals(ReportStatus.COMPLETED, response.status());
        assertEquals(2, response.recordCount());
        assertEquals(sha256(stored.getValue()), response.checksumSha256(),
                "el checksum permite probar ante el supervisor que el archivo no se alteró");
        assertEquals("/v1/compliance/reports/" + response.id() + "/download", response.downloadUrl());
    }

    @Test
    void shouldNotOverwriteAReportThatWasAlreadySubmitted() {
        when(reportPort.findByTypeAndPeriod(SupersolidariaReportType.ASOCIADOS, 2026, 9))
                .thenReturn(Optional.of(report(ReportStatus.COMPLETED)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> generateReportUseCase.execute(officerId, REQUEST));

        assertEquals("REPORT_ALREADY_EXISTS", ex.getCode());
        verify(reportPort, never()).save(any());
        verifyNoInteractions(datasetPort, storagePort);
    }

    @Test
    void shouldRetryAPreviouslyFailedReportInsteadOfCreatingADuplicate() {
        RegulatoryReport failed = report(ReportStatus.FAILED);
        when(reportPort.findByTypeAndPeriod(SupersolidariaReportType.ASOCIADOS, 2026, 9)).thenReturn(Optional.of(failed));
        when(reportPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(datasetPort.buildDataset(any(), eq(2026), eq(9), anyString())).thenReturn(dataset());
        when(storagePort.store(anyString(), any())).thenReturn("/reports/x.txt");

        RegulatoryReportResponse response = generateReportUseCase.execute(officerId, REQUEST);

        assertEquals(failed.getId(), response.id());
        assertEquals(ReportStatus.COMPLETED, response.status());
    }

    @Test
    void shouldMarkReportFailedWhenDataCannotBeBuilt() {
        when(reportPort.findByTypeAndPeriod(SupersolidariaReportType.ASOCIADOS, 2026, 9)).thenReturn(Optional.empty());
        when(reportPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(datasetPort.buildDataset(any(), eq(2026), eq(9), anyString())).thenThrow(new IllegalStateException("consulta falló"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> generateReportUseCase.execute(officerId, REQUEST));

        assertEquals("REPORT_GENERATION_FAILED", ex.getCode());
        ArgumentCaptor<RegulatoryReport> saved = ArgumentCaptor.forClass(RegulatoryReport.class);
        verify(reportPort, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        RegulatoryReport last = saved.getAllValues().get(saved.getAllValues().size() - 1);
        assertEquals(ReportStatus.FAILED, last.getStatus());
        assertEquals("consulta falló", last.getErrorMessage());
        verifyNoInteractions(storagePort);
    }

    private ReportDataset dataset() {
        return ReportDataset.builder()
                .reportType(SupersolidariaReportType.ASOCIADOS)
                .periodYear(2026)
                .periodMonth(9)
                .entityCode("COOP001")
                .headerFields(List.of("DOCUMENTO", "NOMBRE"))
                .detailRows(List.of(
                        ReportDataRow.builder().fields(List.of("52123456", "Ana Pérez")).build(),
                        ReportDataRow.builder().fields(List.of("1086578123", "Juan Ruiz")).build()))
                .build();
    }

    private RegulatoryReport report(ReportStatus status) {
        return RegulatoryReport.builder()
                .id(UUID.randomUUID())
                .reportType(SupersolidariaReportType.ASOCIADOS)
                .periodYear(2026)
                .periodMonth(9)
                .entityCode("COOP001")
                .status(status)
                .build();
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
