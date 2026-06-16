package com.cooperativa.met.domain.compliance.service;

import com.cooperativa.met.domain.compliance.model.ReportDataRow;
import com.cooperativa.met.domain.compliance.model.ReportDataset;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SupersolidariaFlatFileGeneratorTest {

    @Test
    void shouldGenerateFlatFileWithHeaderDetailTrailer() {
        ReportDataset dataset = ReportDataset.builder()
                .reportType(SupersolidariaReportType.ASOCIADOS)
                .periodYear(2026)
                .periodMonth(5)
                .entityCode("COOP001")
                .headerFields(List.of("COL1", "COL2"))
                .detailRows(List.of(
                        ReportDataRow.builder().fields(List.of("1", "CC", "123")).build()
                ))
                .build();

        byte[] content = SupersolidariaFlatFileGenerator.generate(dataset);
        String text = new String(content, StandardCharsets.UTF_8);

        assertTrue(text.startsWith("H|COOP001|ASOCIADOS|"));
        assertTrue(text.contains("D|1|CC|123"));
        assertTrue(text.contains("T|1"));
    }
}
