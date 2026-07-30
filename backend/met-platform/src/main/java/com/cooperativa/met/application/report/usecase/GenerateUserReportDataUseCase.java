package com.cooperativa.met.application.report.usecase;

import com.cooperativa.met.application.report.dto.UserReportData;
import com.cooperativa.met.application.report.dto.UserReportData.InvestmentLine;
import com.cooperativa.met.application.report.dto.UserReportData.LoanLine;
import com.cooperativa.met.application.report.dto.UserReportData.ProjectedYieldLine;
import com.cooperativa.met.application.report.dto.UserReportData.TransactionLine;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Agrega los datos de las 4 secciones del reporte de usuario: movimientos,
 * inversiones activas, préstamos vigentes y rendimientos. No genera el
 * archivo — eso lo hacen {@code PdfReportRenderer}/{@code ExcelReportRenderer}
 * a partir del {@link UserReportData} que devuelve este caso de uso.
 *
 * "Préstamos vigentes" no es un estado propio del dominio (ver
 * {@code LoanApplicationStatus}: no existe ACTIVE/PAID_OFF) — se deriva
 * como DISBURSED con al menos una cuota PENDING o LATE. Una vez todas las
 * cuotas quedan PAID, el préstamo deja de aparecer aquí.
 */
@Service
@RequiredArgsConstructor
public class GenerateUserReportDataUseCase {

    private final UserRepositoryPort userRepository;
    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final MicroInvestmentPort investmentPort;
    private final InvestmentInstrumentPort instrumentPort;
    private final InvestmentReturnPort investmentReturnPort;
    private final PersonalLoanApplicationPort loanApplicationPort;
    private final AmortizationSchedulePort schedulePort;

    @Transactional(readOnly = true)
    public UserReportData execute(UUID userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "La fecha inicial debe ser anterior o igual a la final");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta para el usuario"));

        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<TransactionLine> transactions = buildTransactions(account, start, end);
        List<InvestmentLine> activeInvestments = buildActiveInvestments(userId);
        List<LoanLine> activeLoans = buildActiveLoans(userId);
        List<ProjectedYieldLine> projectedYields = buildProjectedYields(userId, from, to);
        BigDecimal projectedYieldsTotal = projectedYields.stream()
                .map(ProjectedYieldLine::rendimiento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new UserReportData(
                user.getFirstName() + " " + user.getLastName(),
                user.getDocumentNumber(),
                account.getAccountNumber(),
                from,
                to,
                Instant.now(),
                account.getPrincipalBalance(),
                account.getInterestBalance(),
                transactions,
                activeInvestments,
                activeLoans,
                projectedYields,
                projectedYieldsTotal
        );
    }

    private List<TransactionLine> buildTransactions(CoreAccount account, Instant start, Instant end) {
        return transactionRepository.findByAccountId(account.getId()).stream()
                .filter(tx -> !tx.getCreatedAt().isBefore(start) && tx.getCreatedAt().isBefore(end))
                .sorted(Comparator.comparing(CoreTransaction::getCreatedAt))
                .map(tx -> new TransactionLine(
                        tx.getCreatedAt(),
                        tx.getType().name(),
                        tx.getConcept(),
                        tx.getAmount(),
                        account.getId().equals(tx.getDestinationAccountId()) ? "CREDITO" : "DEBITO"))
                .toList();
    }

    private List<InvestmentLine> buildActiveInvestments(UUID userId) {
        return investmentPort.findByUserId(userId).stream()
                .filter(inv -> inv.getEstado() == InvestmentStatus.ACTIVE)
                .map(this::toInvestmentLine)
                .toList();
    }

    private InvestmentLine toInvestmentLine(MicroInvestment inv) {
        String instrumentName = instrumentPort.findById(inv.getInstrumentId())
                .map(i -> i.getNombre())
                .orElse("Instrumento no encontrado");
        return new InvestmentLine(
                instrumentName,
                inv.getMontoInvertido(),
                inv.getTasaAplicada(),
                inv.getFechaInicio(),
                inv.getFechaVencimiento(),
                inv.getRendimientoGanado());
    }

    private List<LoanLine> buildActiveLoans(UUID userId) {
        return loanApplicationPort.findByUserId(userId).stream()
                .filter(app -> app.getStatus() == LoanApplicationStatus.DISBURSED)
                .map(this::toLoanLine)
                .filter(loan -> loan.cuotasPendientes() > 0 || loan.cuotasEnMora() > 0)
                .toList();
    }

    private LoanLine toLoanLine(PersonalLoanApplication app) {
        List<AmortizationInstallment> installments = schedulePort.findByApplicationId(app.getId());

        int pagadas = (int) installments.stream().filter(i -> "PAID".equals(i.getStatus())).count();
        int pendientes = (int) installments.stream().filter(i -> "PENDING".equals(i.getStatus())).count();
        int enMora = (int) installments.stream().filter(i -> "LATE".equals(i.getStatus())).count();
        LocalDate proximo = installments.stream()
                .filter(i -> !"PAID".equals(i.getStatus()))
                .map(AmortizationInstallment::getDueDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        return new LoanLine(
                app.getAmount(),
                app.getTermMonths(),
                app.getMonthlyPayment(),
                pagadas,
                pendientes,
                enMora,
                proximo);
    }

    private List<ProjectedYieldLine> buildProjectedYields(UUID userId, LocalDate from, LocalDate to) {
        return investmentReturnPort.findByUserId(userId).stream()
                .filter(r -> !r.getFechaPago().isBefore(from) && !r.getFechaPago().isAfter(to))
                .sorted(Comparator.comparing(com.cooperativa.met.domain.investment.model.InvestmentReturn::getFechaPago))
                .map(r -> new ProjectedYieldLine(r.getCapital(), r.getRendimiento(), r.getTotalAcreditado(), r.getFechaPago()))
                .toList();
    }
}
