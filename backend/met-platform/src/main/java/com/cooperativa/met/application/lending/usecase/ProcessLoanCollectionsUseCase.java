package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.lending.port.MessagingPort;
import com.cooperativa.met.domain.lending.port.PaymentGatewayPort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessLoanCollectionsUseCase {

    private final AmortizationSchedulePort schedulePort;
    private final PersonalLoanApplicationPort loanPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final MessagingPort messagingPort;

    // Tasa de Usura Actual: 28% Efectiva Anual (Para propósitos del test se deja constante)
    private static final BigDecimal USURY_RATE_ANNUAL = new BigDecimal("0.28");

    @Transactional
    public void execute() {
        LocalDate today = LocalDate.now();
        log.info("[CRON] Iniciando evaluación de cobranza y mora para el día {}", today);

        // 1. Obtener todas las cuotas pendientes que vencen HOY o antes
        List<AmortizationInstallment> dueInstallments = schedulePort.findPendingInstallmentsByDueDateBeforeOrEqual(today);
        
        for (AmortizationInstallment installment : dueInstallments) {
            PersonalLoanApplication loan = loanPort.findById(installment.getApplicationId()).orElse(null);
            if (loan == null) continue;

            User user = userRepositoryPort.findById(loan.getUserId()).orElse(null);
            if (user == null) continue;

            long daysLate = ChronoUnit.DAYS.between(installment.getDueDate(), today);

            // CASO A: Cobro Automático (Vence Hoy o está en mora y tiene token)
            if (user.getPaymentCardToken() != null && !user.getPaymentCardToken().isEmpty()) {
                boolean success = paymentGatewayPort.chargeTokenizedCard(
                        user.getId(),
                        user.getPaymentCardToken(),
                        installment.getPaymentAmount().add(installment.getPenaltyInterestAmount() != null ? installment.getPenaltyInterestAmount() : BigDecimal.ZERO),
                        "Cuota " + installment.getInstallmentNumber() + " de Préstamo " + loan.getId().toString().substring(0,8)
                );

                if (success) {
                    AmortizationInstallment paidInstallment = installment.withStatus("PAID");
                    schedulePort.saveAll(loan.getId(), List.of(paidInstallment));
                    log.info("Cargo exitoso a tarjeta tokenizada para cuota {} del usuario {}", installment.getInstallmentNumber(), user.getId());
                    continue; // Se pagó exitosamente, no entra en mora
                }
            }

            // CASO B: Mora y Penalizaciones
            if (daysLate > 0) {
                // 1. Marcar cuota como LATE
                AmortizationInstallment lateInstallment = installment.withStatus("LATE");

                // 2. Marcar préstamo como MORA si no lo estaba
                // Como LoanApplicationStatus no tiene MORA en este momento, lo omitimos para no romper otros flujos,
                // o lo añadimos si fuera necesario. Para el TEST, el status LATE en la cuota es suficiente.

                // 3. Calcular interés moratorio (Tope Usura)
                // Fórmula diaria simplificada: (Tasa Usura / 360) * Días Mora * Capital
                BigDecimal dailyRate = USURY_RATE_ANNUAL.divide(new BigDecimal("360"), 6, RoundingMode.HALF_UP);
                BigDecimal penalty = installment.getPrincipalAmount()
                        .multiply(dailyRate)
                        .multiply(BigDecimal.valueOf(daysLate))
                        .setScale(2, RoundingMode.HALF_UP);

                lateInstallment = lateInstallment.withPenaltyInterestAmount(penalty);
                schedulePort.saveAll(loan.getId(), List.of(lateInstallment));
                log.info("Cuota {} de usuario {} en mora ({} días). Interés moratorio actual: {}", 
                        installment.getInstallmentNumber(), user.getId(), daysLate, penalty);

                // CASO C: Webhooks de mensajería (Cascada)
                if (daysLate == 1) {
                    messagingPort.sendSms(user.getPhone(), "Hola " + user.getFirstName() + ", recordatorio preventivo: tu cuota venció ayer. Por favor ponte al día para evitar reportes.");
                } else if (daysLate == 5) {
                    messagingPort.sendSms(user.getPhone(), "Alerta " + user.getFirstName() + ": tienes 5 días de mora. A partir del día 6 se generará reporte a centrales de riesgo.");
                } else if (daysLate > 5) {
                    log.info("[DATA CREDITO MOCK] Reportando a central de riesgo usuario {} por {} días de mora.", user.getDocumentNumber(), daysLate);
                }
            }
        }
        
        log.info("[CRON] Evaluación de cobranza y mora finalizada.");
    }
}
