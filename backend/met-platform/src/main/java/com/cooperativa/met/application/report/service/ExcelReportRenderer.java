package com.cooperativa.met.application.report.service;

import com.cooperativa.met.application.report.dto.UserReportData;
import com.cooperativa.met.application.report.dto.UserReportData.InvestmentLine;
import com.cooperativa.met.application.report.dto.UserReportData.LoanLine;
import com.cooperativa.met.application.report.dto.UserReportData.ProjectedYieldLine;
import com.cooperativa.met.application.report.dto.UserReportData.TransactionLine;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renderiza {@link UserReportData} a un libro de Excel (.xlsx) con una hoja
 * por sección, usando Apache POI. Los montos se escriben como celdas
 * numéricas reales (no texto) para que el usuario pueda sumar/filtrar sin
 * tener que reescribir los datos — es la razón de ser de ofrecer Excel
 * además de PDF.
 *
 * NOTA TÉCNICA: se evita deliberadamente {@code Sheet#autoSizeColumn}
 * (requiere AWT/fuentes del sistema operativo) porque el backend corre en
 * una imagen `eclipse-temurin:17-jre-alpine` sin fontconfig — usarlo
 * rompería la generación solo en producción/Docker, no en desarrollo local.
 * Por eso los anchos de columna se fijan a mano.
 */
@Slf4j
@Service
public class ExcelReportRenderer {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] render(UserReportData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle moneyStyle = moneyStyle(workbook);

            writeSummarySheet(workbook, moneyStyle, data);
            writeTransactionsSheet(workbook, headerStyle, moneyStyle, data.transactions());
            writeInvestmentsSheet(workbook, headerStyle, moneyStyle, data.activeInvestments());
            writeLoansSheet(workbook, headerStyle, moneyStyle, data.activeLoans());
            writeYieldsSheet(workbook, headerStyle, moneyStyle, data.projectedYields(), data.projectedYieldsTotal());

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel del reporte de usuario", e);
            throw new RuntimeException("Error generando el reporte en Excel", e);
        }
    }

    private void writeSummarySheet(XSSFWorkbook workbook, CellStyle moneyStyle, UserReportData data) {
        Sheet sheet = workbook.createSheet("Resumen");
        setWidths(sheet, 34, 22);

        int r = 0;
        writeTextRow(sheet, r++, "Usuario", data.userFullName());
        writeTextRow(sheet, r++, "Documento", data.documentNumber());
        writeTextRow(sheet, r++, "Cuenta", data.accountNumber());
        writeTextRow(sheet, r++, "Periodo",
                DATE_FMT.format(data.periodFrom()) + " - " + DATE_FMT.format(data.periodTo()));
        writeTextRow(sheet, r++, "Generado", DATETIME_FMT.format(data.generatedAt().atZone(ZoneId.of("America/Bogota"))));
        r++;
        writeMoneyRow(sheet, moneyStyle, r++, "Saldo disponible (capital)", data.principalBalance());
        writeMoneyRow(sheet, moneyStyle, r++, "Ganancias confirmadas en tu saldo", data.confirmedEarningsBalance());
        writeMoneyRow(sheet, moneyStyle, r++, "Rendimientos registrados al vencimiento (no confirmados aún)", data.projectedYieldsTotal());
    }

    private void writeTransactionsSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle moneyStyle, List<TransactionLine> lines) {
        Sheet sheet = workbook.createSheet("Movimientos");
        setWidths(sheet, 18, 14, 30, 16, 12);
        writeHeader(sheet, headerStyle, 0, "Fecha", "Tipo", "Concepto", "Monto", "Dirección");

        int r = 1;
        for (TransactionLine tx : lines) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(DATETIME_FMT.format(tx.date().atZone(ZoneId.of("America/Bogota"))));
            row.createCell(1).setCellValue(tx.type());
            row.createCell(2).setCellValue(tx.concept() == null ? "" : tx.concept());
            setMoney(row.createCell(3), moneyStyle, tx.amount());
            row.createCell(4).setCellValue(tx.direction());
        }
    }

    private void writeInvestmentsSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle moneyStyle, List<InvestmentLine> lines) {
        Sheet sheet = workbook.createSheet("Inversiones Activas");
        setWidths(sheet, 26, 16, 10, 14, 14, 20);
        writeHeader(sheet, headerStyle, 0, "Instrumento", "Monto invertido", "Tasa anual", "Inicio", "Vencimiento", "Rendimiento acumulado");

        int r = 1;
        for (InvestmentLine inv : lines) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(inv.instrumentName());
            setMoney(row.createCell(1), moneyStyle, inv.montoInvertido());
            row.createCell(2).setCellValue(inv.tasaAplicada() == null ? 0 : inv.tasaAplicada().doubleValue());
            row.createCell(3).setCellValue(DATE_FMT.format(inv.fechaInicio()));
            row.createCell(4).setCellValue(DATE_FMT.format(inv.fechaVencimiento()));
            setMoney(row.createCell(5), moneyStyle, inv.rendimientoAcumulado());
        }
    }

    private void writeLoansSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle moneyStyle, List<LoanLine> lines) {
        Sheet sheet = workbook.createSheet("Prestamos Vigentes");
        setWidths(sheet, 16, 10, 16, 12, 12, 12, 18);
        writeHeader(sheet, headerStyle, 0, "Monto", "Plazo (meses)", "Cuota mensual", "Pagadas", "Pendientes", "En mora", "Próximo vencimiento");

        int r = 1;
        for (LoanLine loan : lines) {
            Row row = sheet.createRow(r++);
            setMoney(row.createCell(0), moneyStyle, loan.amount());
            row.createCell(1).setCellValue(loan.termMonths());
            setMoney(row.createCell(2), moneyStyle, loan.monthlyPayment());
            row.createCell(3).setCellValue(loan.cuotasPagadas());
            row.createCell(4).setCellValue(loan.cuotasPendientes());
            row.createCell(5).setCellValue(loan.cuotasEnMora());
            row.createCell(6).setCellValue(loan.proximoVencimiento() == null ? "-" : DATE_FMT.format(loan.proximoVencimiento()));
        }
    }

    private void writeYieldsSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle moneyStyle, List<ProjectedYieldLine> lines, BigDecimal total) {
        Sheet sheet = workbook.createSheet("Rendimientos");
        setWidths(sheet, 16, 16, 18, 14);

        Row note = sheet.createRow(0);
        note.createCell(0).setCellValue(
                "Registrados al vencer una inversión — aún NO reflejados en tu saldo disponible (en proceso de conciliación).");

        writeHeader(sheet, headerStyle, 2, "Capital", "Rendimiento", "Total registrado", "Fecha de pago");

        int r = 3;
        for (ProjectedYieldLine y : lines) {
            Row row = sheet.createRow(r++);
            setMoney(row.createCell(0), moneyStyle, y.capital());
            setMoney(row.createCell(1), moneyStyle, y.rendimiento());
            setMoney(row.createCell(2), moneyStyle, y.totalAcreditado());
            row.createCell(3).setCellValue(DATE_FMT.format(y.fechaPago()));
        }
        Row totalRow = sheet.createRow(r + 1);
        totalRow.createCell(0).setCellValue("Total del periodo");
        setMoney(totalRow.createCell(1), moneyStyle, total);
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle, int rowIndex, String... headers) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeTextRow(Sheet sheet, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void writeMoneyRow(Sheet sheet, CellStyle moneyStyle, int rowIndex, String label, BigDecimal amount) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        setMoney(row.createCell(1), moneyStyle, amount);
    }

    private void setMoney(Cell cell, CellStyle moneyStyle, BigDecimal amount) {
        cell.setCellValue(amount == null ? 0 : amount.doubleValue());
        cell.setCellStyle(moneyStyle);
    }

    private void setWidths(Sheet sheet, int... charsPerColumn) {
        for (int i = 0; i < charsPerColumn.length; i++) {
            sheet.setColumnWidth(i, charsPerColumn[i] * 256);
        }
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle moneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return style;
    }
}
