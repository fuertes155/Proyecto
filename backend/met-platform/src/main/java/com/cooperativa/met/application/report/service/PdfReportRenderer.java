package com.cooperativa.met.application.report.service;

import com.cooperativa.met.application.report.dto.UserReportData;
import com.cooperativa.met.application.report.dto.UserReportData.InvestmentLine;
import com.cooperativa.met.application.report.dto.UserReportData.LoanLine;
import com.cooperativa.met.application.report.dto.UserReportData.ProjectedYieldLine;
import com.cooperativa.met.application.report.dto.UserReportData.TransactionLine;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renderiza {@link UserReportData} a PDF usando OpenPDF (mismo motor que
 * {@code PdfGeneratorService} ya usa para el contrato de mandato).
 */
@Slf4j
@Service
public class PdfReportRenderer {

    private static final Locale CO = new Locale("es", "CO");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font NOTE_FONT = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.DARK_GRAY);

    public byte[] render(UserReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Reporte de cuenta", TITLE_FONT));
            document.add(new Paragraph(
                    data.userFullName() + "  ·  Documento " + data.documentNumber() + "  ·  Cuenta " + data.accountNumber(),
                    SUBTITLE_FONT));
            document.add(new Paragraph(
                    "Periodo: " + DATE_FMT.format(data.periodFrom()) + " - " + DATE_FMT.format(data.periodTo())
                            + "   ·   Generado: " + DATETIME_FMT.format(data.generatedAt().atZone(ZoneId.of("America/Bogota"))),
                    SUBTITLE_FONT));
            document.add(new Paragraph(" "));

            addBalanceSummary(document, data);
            addTransactionsSection(document, data.transactions());
            addInvestmentsSection(document, data.activeInvestments());
            addLoansSection(document, data.activeLoans());
            addYieldsSection(document, data.projectedYields(), data.projectedYieldsTotal());

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF del reporte de usuario", e);
            throw new RuntimeException("Error generando el reporte en PDF", e);
        }
    }

    private void addBalanceSummary(Document document, UserReportData data) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(borderlessCell("Saldo disponible (capital): " + cop(data.principalBalance()), CELL_FONT));
        table.addCell(borderlessCell("Ganancias confirmadas en tu saldo: " + cop(data.confirmedEarningsBalance()), CELL_FONT));
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addTransactionsSection(Document document, List<TransactionLine> lines) throws Exception {
        document.add(new Paragraph("Historial de movimientos", SECTION_FONT));
        if (lines.isEmpty()) {
            document.add(new Paragraph("Sin movimientos en el periodo seleccionado.", CELL_FONT));
            document.add(new Paragraph(" "));
            return;
        }
        PdfPTable table = new PdfPTable(new float[]{2.2f, 1.3f, 3f, 1.5f, 1.2f});
        table.setWidthPercentage(100);
        addHeaderRow(table, "Fecha", "Tipo", "Concepto", "Monto", "Dirección");
        for (TransactionLine tx : lines) {
            table.addCell(dataCell(DATETIME_FMT.format(tx.date().atZone(ZoneId.of("America/Bogota")))));
            table.addCell(dataCell(tx.type()));
            table.addCell(dataCell(tx.concept() == null ? "" : tx.concept()));
            table.addCell(dataCell(cop(tx.amount())));
            table.addCell(dataCell(tx.direction()));
        }
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addInvestmentsSection(Document document, List<InvestmentLine> lines) throws Exception {
        document.add(new Paragraph("Inversiones activas", SECTION_FONT));
        if (lines.isEmpty()) {
            document.add(new Paragraph("No tienes inversiones activas en este momento.", CELL_FONT));
            document.add(new Paragraph(" "));
            return;
        }
        PdfPTable table = new PdfPTable(new float[]{2.5f, 1.5f, 1f, 1.3f, 1.3f, 1.5f});
        table.setWidthPercentage(100);
        addHeaderRow(table, "Instrumento", "Monto invertido", "Tasa", "Inicio", "Vencimiento", "Rendimiento acumulado");
        for (InvestmentLine inv : lines) {
            table.addCell(dataCell(inv.instrumentName()));
            table.addCell(dataCell(cop(inv.montoInvertido())));
            table.addCell(dataCell(percent(inv.tasaAplicada())));
            table.addCell(dataCell(DATE_FMT.format(inv.fechaInicio())));
            table.addCell(dataCell(DATE_FMT.format(inv.fechaVencimiento())));
            table.addCell(dataCell(cop(inv.rendimientoAcumulado())));
        }
        document.add(table);
        document.add(new Paragraph("Rendimiento acumulado: dinero real, ya reflejado en tu saldo de ganancias.", NOTE_FONT));
        document.add(new Paragraph(" "));
    }

    private void addLoansSection(Document document, List<LoanLine> lines) throws Exception {
        document.add(new Paragraph("Préstamos vigentes", SECTION_FONT));
        if (lines.isEmpty()) {
            document.add(new Paragraph("No tienes préstamos vigentes en este momento.", CELL_FONT));
            document.add(new Paragraph(" "));
            return;
        }
        PdfPTable table = new PdfPTable(new float[]{1.5f, 1f, 1.5f, 1f, 1f, 1f, 1.3f});
        table.setWidthPercentage(100);
        addHeaderRow(table, "Monto", "Plazo", "Cuota mensual", "Pagadas", "Pendientes", "En mora", "Próximo vencimiento");
        for (LoanLine loan : lines) {
            table.addCell(dataCell(cop(loan.amount())));
            table.addCell(dataCell(loan.termMonths() + " meses"));
            table.addCell(dataCell(cop(loan.monthlyPayment())));
            table.addCell(dataCell(String.valueOf(loan.cuotasPagadas())));
            table.addCell(dataCell(String.valueOf(loan.cuotasPendientes())));
            table.addCell(dataCell(String.valueOf(loan.cuotasEnMora())));
            table.addCell(dataCell(loan.proximoVencimiento() == null ? "-" : DATE_FMT.format(loan.proximoVencimiento())));
        }
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addYieldsSection(Document document, List<ProjectedYieldLine> lines, BigDecimal total) throws Exception {
        document.add(new Paragraph("Rendimientos registrados al vencimiento", SECTION_FONT));
        document.add(new Paragraph(
                "Estos rendimientos quedaron registrados al vencer una inversión, pero a la fecha de este reporte "
                        + "aún NO están reflejados en tu saldo disponible — están en proceso de conciliación. "
                        + "No los sumes al saldo de ganancias confirmadas de arriba.",
                NOTE_FONT));
        if (lines.isEmpty()) {
            document.add(new Paragraph("Sin rendimientos registrados en el periodo seleccionado.", CELL_FONT));
            return;
        }
        PdfPTable table = new PdfPTable(new float[]{1.5f, 1.5f, 1.5f, 1.3f});
        table.setWidthPercentage(100);
        addHeaderRow(table, "Capital", "Rendimiento", "Total registrado", "Fecha de pago");
        for (ProjectedYieldLine y : lines) {
            table.addCell(dataCell(cop(y.capital())));
            table.addCell(dataCell(cop(y.rendimiento())));
            table.addCell(dataCell(cop(y.totalAcreditado())));
            table.addCell(dataCell(DATE_FMT.format(y.fechaPago())));
        }
        document.add(table);
        document.add(new Paragraph("Total rendimientos registrados en el periodo: " + cop(total), CELL_FONT));
    }

    private void addHeaderRow(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(new Color(0x2E, 0x7D, 0x32));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private PdfPCell dataCell(String text) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, CELL_FONT));
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell borderlessCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBorder(Element.RECTANGLE);
        cell.setPadding(6);
        return cell;
    }

    private String cop(BigDecimal amount) {
        if (amount == null) return "$0";
        return "$" + NumberFormat.getIntegerInstance(CO).format(amount);
    }

    private String percent(BigDecimal rate) {
        if (rate == null) return "-";
        return NumberFormat.getPercentInstance(CO).format(rate) + " E.A.";
    }
}
