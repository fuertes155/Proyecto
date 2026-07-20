package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminLoanUseCase {

    private final PersonalLoanApplicationPort loanApplicationPort;
    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final NotificationRepositoryPort notificationRepository;
    private final FeeScheduleRepositoryPort feeRepository;

    public List<PersonalLoanApplication> getAllLoans() {
        return loanApplicationPort.findAll();
    }

    @Transactional
    public PersonalLoanApplication updateLoanStatus(UUID loanId, LoanApplicationStatus status) {
        PersonalLoanApplication loan = loanApplicationPort.findById(loanId)
                .orElseThrow(() -> new BusinessRuleException("LOAN_NOT_FOUND", "Solicitud de crédito no encontrada"));

        // Si se aprueba, realizar el desembolso
        if (status == LoanApplicationStatus.APPROVED && loan.getStatus() != LoanApplicationStatus.APPROVED) {
            BigDecimal fondoGarantias = feeRepository.findVigentes().stream()
                .filter(f -> "FONDO_GARANTIAS".equals(f.getTipoTarifa()))
                .findFirst()
                .map(FeeSchedule::getValor)
                .orElse(new BigDecimal("15000.00")); // Fallback seguro
                
            BigDecimal netDisbursement = loan.getAmount().subtract(fondoGarantias);

            CoreAccount account = accountRepository.findByUserId(loan.getUserId())
                    .orElseThrow(() -> new BusinessRuleException("NO_ACCOUNT", "El usuario no tiene billetera virtual para desembolsar"));
            
            // Fix: Re-validate the 10x collateral rule at the time of approval
            BigDecimal maxLoanAmount = account.getPrincipalBalance().multiply(new BigDecimal("10"));
            if (loan.getAmount().compareTo(maxLoanAmount) > 0) {
                throw new BusinessRuleException("MAX_LOAN_EXCEEDED", 
                        "El usuario ya no tiene saldo suficiente para respaldar este préstamo. Saldo actual: $" + account.getPrincipalBalance());
            }

            CoreAccount updatedAccount = account.creditPrincipal(netDisbursement);
            accountRepository.save(updatedAccount);

            CoreTransaction disbursementTx = CoreTransaction.builder()
                    .id(UUID.randomUUID())
                    .sourceAccountId(updatedAccount.getId())
                    .destinationAccountId(updatedAccount.getId())
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.COMPLETED)
                    .amount(netDisbursement)
                    .concept("Desembolso Préstamo (Neto de FGA: -$15,000)")
                    .createdAt(Instant.now())
                    .build();
            transactionRepository.save(disbursementTx);
        }

        // Send notification to user if status changed to APPROVED or REJECTED
        if (status != loan.getStatus() && (status == LoanApplicationStatus.APPROVED || status == LoanApplicationStatus.REJECTED)) {
            String title = status == LoanApplicationStatus.APPROVED ? "Préstamo Aprobado 🎉" : "Préstamo Rechazado ❌";
            String message = status == LoanApplicationStatus.APPROVED 
                ? "Tu solicitud de préstamo por $" + loan.getAmount() + " ha sido aprobada y desembolsada."
                : "Lo sentimos, tu solicitud de préstamo por $" + loan.getAmount() + " no fue aprobada en esta ocasión.";
                
            Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(loan.getUserId())
                .title(title)
                .message(message)
                .read(false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
            notificationRepository.save(notification);
        }

        PersonalLoanApplication updated = loan.toBuilder().status(status).build();
        return loanApplicationPort.save(updated);
    }
}
