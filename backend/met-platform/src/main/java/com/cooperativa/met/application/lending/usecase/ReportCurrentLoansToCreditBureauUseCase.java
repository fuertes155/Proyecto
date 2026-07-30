package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.CreditReportEvent;
import com.cooperativa.met.domain.lending.model.CreditReportEventType;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Reporta mensualmente a la central de riesgo que los préstamos vigentes están
 * "al día" — la contraparte de {@link ProcessLoanCollectionsUseCase}, que reporta
 * mora, y de {@code reportFullPayoffIfApplicable}, que reporta pagos completos.
 * Cierra el ciclo de "vida crediticia": consulta (módulo de scoring) + reporte
 * periódico del comportamiento de pago (Ley 1266 de 2008).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCurrentLoansToCreditBureauUseCase {

    private final PersonalLoanApplicationPort loanPort;
    private final AmortizationSchedulePort schedulePort;
    private final UserRepositoryPort userRepositoryPort;
    private final CreditBureauPort creditBureauPort;

    @Transactional(readOnly = true)
    public void execute() {
        LocalDate today = LocalDate.now();
        List<PersonalLoanApplication> activeLoans = loanPort.findByStatus(LoanApplicationStatus.APPROVED);
        log.info("[CRON] Reportando estado 'al día' de {} préstamos vigentes a la central de riesgo", activeLoans.size());

        for (PersonalLoanApplication loan : activeLoans) {
            List<AmortizationInstallment> installments = schedulePort.findByApplicationId(loan.getId());

            boolean hasLateInstallment = installments.stream().anyMatch(i -> "LATE".equals(i.getStatus()));
            if (hasLateInstallment) {
                // Ya se reporta como MORA desde el job diario de cobranza; evitamos reportar doble.
                continue;
            }

            User user = userRepositoryPort.findById(loan.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }

            BigDecimal outstandingBalance = installments.stream()
                    .filter(i -> "PAID".equals(i.getStatus()))
                    .max(Comparator.comparingInt(AmortizationInstallment::getInstallmentNumber))
                    .map(AmortizationInstallment::getRemainingBalance)
                    .orElse(loan.getAmount());

            creditBureauPort.reportCreditBehavior(CreditReportEvent.builder()
                    .userId(user.getId())
                    .nationalId(user.getDocumentNumber())
                    .loanId(loan.getId())
                    .eventType(CreditReportEventType.AL_DIA)
                    .outstandingBalance(outstandingBalance)
                    .reportedAt(today)
                    .build());
        }

        log.info("[CRON] Reporte mensual de vida crediticia finalizado.");
    }
}
