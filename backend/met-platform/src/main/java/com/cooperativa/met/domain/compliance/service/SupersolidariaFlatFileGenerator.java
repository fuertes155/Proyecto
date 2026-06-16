package com.cooperativa.met.domain.compliance.service;

import com.cooperativa.met.domain.compliance.model.ReportDataset;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class SupersolidariaFlatFileGenerator {

    private static final String DELIMITER = "|";
    private static final String VERSION = "1.0";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private SupersolidariaFlatFileGenerator() {
    }

    public static byte[] generate(ReportDataset dataset) {
        List<String> lines = new ArrayList<>();

        String period = String.format("%04d%02d", dataset.getPeriodYear(), dataset.getPeriodMonth());
        lines.add(join("H", dataset.getEntityCode(), dataset.getReportType().name(), period,
                LocalDate.now().format(DATE_FMT), VERSION));

        if (dataset.getHeaderFields() != null && !dataset.getHeaderFields().isEmpty()) {
            lines.add("C" + DELIMITER + String.join(DELIMITER, dataset.getHeaderFields()));
        }

        for (var row : dataset.getDetailRows()) {
            lines.add("D" + DELIMITER + row.toFlatLine(DELIMITER));
        }

        lines.add(join("T", String.valueOf(dataset.getDetailRows().size())));

        String content = String.join("\r\n", lines) + "\r\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static String buildFileName(ReportDataset dataset) {
        String period = String.format("%04d%02d", dataset.getPeriodYear(), dataset.getPeriodMonth());
        return String.format("SUPERSOL_%s_%s_%s.txt",
                dataset.getEntityCode(), dataset.getReportType().name(), period);
    }

    private static String join(String... parts) {
        return String.join(DELIMITER, parts);
    }
}
