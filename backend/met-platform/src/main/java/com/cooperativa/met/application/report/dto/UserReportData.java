package com.cooperativa.met.application.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos agregados del reporte de un usuario, ya listos para ser renderizados
 * a PDF o Excel — ver {@code GenerateUserReportDataUseCase} para cómo se
 * arma cada sección.
 *
 * IMPORTANTE sobre rendimientos: {@code confirmedEarningsBalance} es el
 * único número 100% respaldado por saldo real (interestBalance de
 * CoreAccount, alimentado por el job de acumulación diaria). Los valores en
 * {@code projectedYields} vienen de investment_returns (pago al vencimiento)
 * y hoy NO están reflejados en el saldo real — ver disclaimer obligatorio
 * en cada renderer. No fusionar ambos números.
 */
public record UserReportData(
        String userFullName,
        String documentNumber,
        String accountNumber,
        LocalDate periodFrom,
        LocalDate periodTo,
        Instant generatedAt,
        BigDecimal principalBalance,
        BigDecimal confirmedEarningsBalance,
        List<TransactionLine> transactions,
        List<InvestmentLine> activeInvestments,
        List<LoanLine> activeLoans,
        List<ProjectedYieldLine> projectedYields,
        BigDecimal projectedYieldsTotal
) {
    public record TransactionLine(
            Instant date,
            String type,
            String concept,
            BigDecimal amount,
            String direction
    ) {
    }

    public record InvestmentLine(
            String instrumentName,
            BigDecimal montoInvertido,
            BigDecimal tasaAplicada,
            LocalDate fechaInicio,
            LocalDate fechaVencimiento,
            BigDecimal rendimientoAcumulado
    ) {
    }

    public record LoanLine(
            BigDecimal amount,
            int termMonths,
            BigDecimal monthlyPayment,
            int cuotasPagadas,
            int cuotasPendientes,
            int cuotasEnMora,
            LocalDate proximoVencimiento
    ) {
    }

    public record ProjectedYieldLine(
            BigDecimal capital,
            BigDecimal rendimiento,
            BigDecimal totalAcreditado,
            LocalDate fechaPago
    ) {
    }
}
