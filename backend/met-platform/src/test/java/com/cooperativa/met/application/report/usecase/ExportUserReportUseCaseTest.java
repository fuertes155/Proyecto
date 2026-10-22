package com.cooperativa.met.application.report.usecase;

import com.cooperativa.met.application.report.dto.ReportFileResult;
import com.cooperativa.met.application.report.dto.UserReportData;
import com.cooperativa.met.application.report.service.ExcelReportRenderer;
import com.cooperativa.met.application.report.service.PdfReportRenderer;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Reporte de cuenta del socio en PDF y Excel, de punta a punta: datos reales del período, render
 * real con OpenPDF/POI, y lectura del archivo generado para verificar su contenido.
 */
@ExtendWith(MockitoExtension.class)
class ExportUserReportUseCaseTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    @Mock private UserRepositoryPort userRepository;
    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private CoreTransactionRepositoryPort transactionRepository;
    @Mock private MicroInvestmentPort investmentPort;
    @Mock private InvestmentInstrumentPort instrumentPort;
    @Mock private InvestmentReturnPort investmentReturnPort;
    @Mock private PersonalLoanApplicationPort loanApplicationPort;
    @Mock private AmortizationSchedulePort schedulePort;

    private GenerateUserReportDataUseCase dataUseCase;
    private ExportUserReportUseCase exportUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID loanId = UUID.randomUUID();
    private final UUID instrumentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dataUseCase = new GenerateUserReportDataUseCase(userRepository, accountRepository, transactionRepository, investmentPort,
                instrumentPort, investmentReturnPort, loanApplicationPort, schedulePort);
        exportUseCase = new ExportUserReportUseCase(dataUseCase, new PdfReportRenderer(), new ExcelReportRenderer());
    }

    @Test
    void shouldGatherOnlyWhatBelongsToThePeriodAndWhatIsStillActive() {
        givenFullHistory();

        UserReportData data = dataUseCase.execute(userId, FROM, TO);

        assertEquals("Ana Pérez", data.userFullName());
        assertEquals(List.of("Recarga septiembre", "Pago arriendo"),
                data.transactions().stream().map(UserReportData.TransactionLine::concept).toList());
        assertEquals("CREDITO", data.transactions().get(0).direction());
        assertEquals("DEBITO", data.transactions().get(1).direction());
        assertEquals(List.of("CDT 90 días"), data.activeInvestments().stream().map(UserReportData.InvestmentLine::instrumentName).toList());

        assertEquals(1, data.activeLoans().size(), "un préstamo totalmente pagado ya no es vigente");
        UserReportData.LoanLine loan = data.activeLoans().get(0);
        assertEquals(1, loan.cuotasPagadas());
        assertEquals(1, loan.cuotasPendientes());
        assertEquals(1, loan.cuotasEnMora());
        assertEquals(LocalDate.of(2026, 9, 15), loan.proximoVencimiento(), "la cuota en mora es la próxima que vence");

        assertEquals(1, data.projectedYields().size());
        assertEquals(new BigDecimal("12500.00"), data.projectedYieldsTotal());
    }

    @Test
    void shouldRenderAReadablePdf() throws Exception {
        givenFullHistory();

        ReportFileResult pdf = exportUseCase.execute(userId, FROM, TO, "PDF");

        assertEquals("application/pdf", pdf.contentType());
        assertEquals("reporte_1000012345_2026-09-01_a_2026-09-30.pdf", pdf.fileName());
        String text = pdfText(pdf.content());
        assertTrue(text.contains("Reporte de cuenta"), text);
        assertTrue(text.contains("Ana Pérez"), text);
        assertTrue(text.contains("Pago arriendo"), text);
        assertTrue(text.contains("CDT 90 días"), text);
        assertTrue(text.contains("Préstamos vigentes"), text);
    }

    @Test
    void shouldRenderAnExcelWithOneSheetPerSection() throws Exception {
        givenFullHistory();

        ReportFileResult excel = exportUseCase.execute(userId, FROM, TO, "xlsx");

        assertTrue(excel.fileName().endsWith(".xlsx"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.content()))) {
            assertEquals(List.of("Resumen", "Movimientos", "Inversiones Activas", "Prestamos Vigentes", "Rendimientos"),
                    List.of(workbook.getSheetAt(0).getSheetName(), workbook.getSheetAt(1).getSheetName(),
                            workbook.getSheetAt(2).getSheetName(), workbook.getSheetAt(3).getSheetName(),
                            workbook.getSheetAt(4).getSheetName()));
            Sheet movements = workbook.getSheet("Movimientos");
            assertEquals(3, movements.getPhysicalNumberOfRows(), "encabezado + 2 movimientos del período");
            assertEquals("Recarga septiembre", cellText(movements.getRow(1), 2));
            assertTrue(sheetText(workbook.getSheet("Resumen")).contains("Ana Pérez"));
        }
    }

    @Test
    void shouldStillRenderBothFormatsForANewAccountWithNoActivity() throws Exception {
        givenUserAndAccount();
        when(transactionRepository.findByAccountId(accountId)).thenReturn(List.of());
        when(investmentPort.findByUserId(userId)).thenReturn(List.of());
        when(loanApplicationPort.findByUserId(userId)).thenReturn(List.of());
        when(investmentReturnPort.findByUserId(userId)).thenReturn(List.of());

        String text = pdfText(exportUseCase.execute(userId, FROM, TO, null).content());
        ReportFileResult excel = exportUseCase.execute(userId, FROM, TO, "excel");

        assertTrue(text.contains("Sin movimientos en el periodo seleccionado."), text);
        assertTrue(text.contains("No tienes inversiones activas en este momento."), text);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.content()))) {
            assertEquals(5, workbook.getNumberOfSheets());
        }
    }

    @Test
    void shouldRejectUnknownFormatAndInvertedDates() {
        givenUserAndAccount();
        when(transactionRepository.findByAccountId(accountId)).thenReturn(List.of());
        when(investmentPort.findByUserId(userId)).thenReturn(List.of());
        when(loanApplicationPort.findByUserId(userId)).thenReturn(List.of());
        when(investmentReturnPort.findByUserId(userId)).thenReturn(List.of());

        assertEquals("INVALID_FORMAT", assertThrows(BusinessRuleException.class,
                () -> exportUseCase.execute(userId, FROM, TO, "docx")).getCode());
        assertEquals("INVALID_DATE_RANGE", assertThrows(BusinessRuleException.class,
                () -> dataUseCase.execute(userId, TO, FROM)).getCode());
    }

    @Test
    void shouldFailForUserWithoutAccount() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dataUseCase.execute(userId, FROM, TO));
    }

    private void givenFullHistory() {
        givenUserAndAccount();
        UUID other = UUID.randomUUID();
        when(transactionRepository.findByAccountId(accountId)).thenReturn(List.of(
                tx(TransactionType.TRANSFER, accountId, other, "2026-09-20T15:00:00Z", "Pago arriendo"),
                tx(TransactionType.DEPOSIT, null, accountId, "2026-09-02T10:00:00Z", "Recarga septiembre"),
                tx(TransactionType.DEPOSIT, null, accountId, "2026-08-31T23:00:00Z", "Recarga agosto"),
                tx(TransactionType.DEPOSIT, null, accountId, "2026-10-01T00:00:00Z", "Recarga octubre")));

        when(investmentPort.findByUserId(userId)).thenReturn(List.of(
                investment(InvestmentStatus.ACTIVE), investment(InvestmentStatus.COMPLETED)));
        when(instrumentPort.findById(instrumentId)).thenReturn(Optional.of(
                InvestmentInstrument.builder().id(instrumentId).nombre("CDT 90 días").build()));

        UUID paidOffLoan = UUID.randomUUID();
        when(loanApplicationPort.findByUserId(userId)).thenReturn(List.of(
                loan(loanId, LoanApplicationStatus.DISBURSED), loan(paidOffLoan, LoanApplicationStatus.DISBURSED),
                loan(UUID.randomUUID(), LoanApplicationStatus.REJECTED)));
        when(schedulePort.findByApplicationId(loanId)).thenReturn(List.of(
                installment(1, "PAID", "2026-08-15"), installment(2, "LATE", "2026-09-15"), installment(3, "PENDING", "2026-10-15")));
        when(schedulePort.findByApplicationId(paidOffLoan)).thenReturn(List.of(installment(1, "PAID", "2026-08-15")));

        when(investmentReturnPort.findByUserId(userId)).thenReturn(List.of(
                yieldPaid("2026-09-10", "12500.00"), yieldPaid("2026-07-10", "9000.00")));
    }

    private void givenUserAndAccount() {
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId).firstName("Ana").lastName("Pérez").documentNumber("1086578123").build()));
        lenient().when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(CoreAccount.builder()
                .id(accountId).userId(userId).accountNumber("1000012345").principalBalance(new BigDecimal("1200000"))
                .interestBalance(new BigDecimal("35000")).status(AccountStatus.ACTIVE).build()));
    }

    private CoreTransaction tx(TransactionType type, UUID source, UUID destination, String at, String concept) {
        return CoreTransaction.builder()
                .id(UUID.randomUUID()).sourceAccountId(source).destinationAccountId(destination)
                .amount(new BigDecimal("150000")).concept(concept).type(type).status(TransactionStatus.COMPLETED)
                .createdAt(Instant.parse(at)).build();
    }

    private MicroInvestment investment(InvestmentStatus status) {
        return MicroInvestment.builder()
                .id(UUID.randomUUID()).instrumentId(instrumentId).userId(userId).montoInvertido(new BigDecimal("500000"))
                .tasaAplicada(new BigDecimal("0.0950")).plazoDias(90).fechaInicio(LocalDate.of(2026, 8, 1))
                .fechaVencimiento(LocalDate.of(2026, 10, 30)).rendimientoGanado(new BigDecimal("7808.22")).estado(status).build();
    }

    private PersonalLoanApplication loan(UUID id, LoanApplicationStatus status) {
        return PersonalLoanApplication.builder()
                .id(id).userId(userId).amount(new BigDecimal("3000000")).termMonths(3)
                .monthlyPayment(new BigDecimal("1030000")).status(status).build();
    }

    private AmortizationInstallment installment(int number, String status, String dueDate) {
        return AmortizationInstallment.builder()
                .id(UUID.randomUUID()).installmentNumber(number).status(status).dueDate(LocalDate.parse(dueDate)).build();
    }

    private InvestmentReturn yieldPaid(String date, String rendimiento) {
        return InvestmentReturn.builder()
                .id(UUID.randomUUID()).userId(userId).capital(new BigDecimal("500000")).rendimiento(new BigDecimal(rendimiento))
                .totalAcreditado(new BigDecimal("500000").add(new BigDecimal(rendimiento))).fechaPago(LocalDate.parse(date)).build();
    }

    private static String pdfText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private static String cellText(Row row, int column) {
        return row.getCell(column).getStringCellValue();
    }

    private static String sheetText(Sheet sheet) {
        StringBuilder text = new StringBuilder();
        sheet.forEach(row -> row.forEach(cell -> text.append(cell).append(' ')));
        return text.toString();
    }
}
