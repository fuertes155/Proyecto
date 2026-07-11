package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.met.application.lending.dto.SubmitLoanApplicationRequest;
import com.cooperativa.met.application.lending.mapper.LendingMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.LoanSimulationResult;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.domain.lending.service.FrenchAmortizationCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmitPersonalLoanApplicationUseCase {

    private final UserRepositoryPort userRepository;
    private final PersonalLoanApplicationPort applicationPort;
    private final AmortizationSchedulePort schedulePort;
    private final ComplianceCheckPort complianceCheckPort;
    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final LendingMapper mapper;

    @Transactional
    public LoanApplicationResponse execute(UUID userId, SubmitLoanApplicationRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Usuario no encontrado"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "Cuenta no activa");
        }

        if (applicationPort.hasPendingApplication(userId)) {
            throw new BusinessRuleException("PENDING_APPLICATION_EXISTS",
                    "Ya tienes una solicitud de préstamo en trámite");
        }

        for (ComplianceListType listType : ComplianceListType.values()) {
            ComplianceResult result = complianceCheckPort.checkUser(userId, listType);
            complianceCheckPort.persistCheck(userId, listType, result, "LOAN_APPLICATION");
            if (result == ComplianceResult.MATCH) {
                throw new BusinessRuleException("COMPLIANCE_MATCH",
                        "No es posible solicitar préstamo: lista restrictiva " + listType);
            }
        }

        // Capa 3: Motor de Riesgo (Filtro Externo simulado - DataCrédito/TransUnion)
        int simulatedRiskScore = (int) (Math.random() * 1000); // 0 to 1000
        if (simulatedRiskScore < 600) {
            throw new BusinessRuleException("RISK_SCORE_LOW", 
                    "Tu solicitud ha sido rechazada por nuestro motor de riesgo (Score: " + simulatedRiskScore + ").");
        }

        LoanSimulationResult simulation = FrenchAmortizationCalculator.simulate(
                request.amount(),
                request.termMonths(),
                request.annualInterestRate(),
                LocalDate.now()
        );

        UUID applicationId = UUID.randomUUID();
        PersonalLoanApplication application = PersonalLoanApplication.builder()
                .id(applicationId)
                .userId(userId)
                .amount(request.amount())
                .termMonths(request.termMonths())
                .annualInterestRate(simulation.getAnnualInterestRate())
                .monthlyPayment(simulation.getMonthlyPayment())
                .totalInterest(simulation.getTotalInterest())
                .totalPayment(simulation.getTotalPayment())
                .purpose(request.purpose())
                .status(LoanApplicationStatus.APPROVED) // Aprobado automáticamente
                .submittedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PersonalLoanApplication saved = applicationPort.save(application);

        List<AmortizationInstallment> schedule = schedulePort.saveAll(
                applicationId,
                simulation.getSchedule()
        );

        // Capa 1: Desembolso automático descontando el "Fondo de Garantías" (15.000 COP)
        BigDecimal fondoGarantias = new BigDecimal("15000.00");
        BigDecimal netDisbursement = request.amount().subtract(fondoGarantias);

        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("NO_ACCOUNT", "El usuario no tiene billetera virtual para desembolsar"));
        
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

        return mapper.toResponse(saved, schedule);
    }
}
