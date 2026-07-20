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
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import com.cooperativa.met.domain.lending.model.CreditScoreResult;
import com.cooperativa.met.domain.lending.service.FrenchAmortizationCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final CreditBureauPort creditBureauPort;
    private final LendingMapper mapper;

    @Value("${met.credit-bureau.min-score:600}")
    private int minCreditScore;

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
            complianceCheckPort.persistCheck(userId, listType, result, "{\"context\": \"LOAN_APPLICATION\"}");
            if (result == ComplianceResult.MATCH) {
                throw new BusinessRuleException("COMPLIANCE_MATCH",
                        "No es posible solicitar préstamo: lista restrictiva " + listType);
            }
        }

        // Validación de Habeas Data
        if (request.hasAcceptedHabeasData() == null || !request.hasAcceptedHabeasData()) {
            throw new BusinessRuleException("HABEAS_DATA_REQUIRED", "Debes aceptar los términos de Habeas Data para consultar tu reporte de crédito.");
        }

        // Capa 3: Motor de Riesgo (Filtro Externo - DataCrédito Experian)
        CreditScoreResult scoreResult = creditBureauPort.checkScore(
                userId,
                user.getDocumentNumber(),
                user.getFirstName(),
                user.getLastName(),
                null // User doesn't have dateOfBirth yet
        );

        if (scoreResult.getScore() < minCreditScore) {
            throw new BusinessRuleException("RISK_SCORE_LOW", 
                    "Tu solicitud ha sido rechazada por nuestro motor de riesgo (Score: " + scoreResult.getScore() + ").");
        }

        // Capa 4: Validación de Saldo (Ahorro)
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("NO_ACCOUNT", "El usuario no tiene billetera virtual para validar el saldo"));
        
        BigDecimal maxLoanAmount = account.getPrincipalBalance().multiply(new BigDecimal("10"));
        if (request.amount().compareTo(maxLoanAmount) > 0) {
            throw new BusinessRuleException("MAX_LOAN_EXCEEDED", 
                    "El monto solicitado excede tu límite. Puedes pedir máximo 10 veces tu saldo actual.");
        }

        BigDecimal baseInterestRate = new BigDecimal("0.15"); // 15% annual rate
        
        LoanSimulationResult simulation = FrenchAmortizationCalculator.simulate(
                request.amount(),
                request.termMonths(),
                baseInterestRate,
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
                .status(LoanApplicationStatus.IN_REVIEW) // Pasa a revisión manual
                .creditScore(scoreResult.getScore())
                .creditBureauRef(scoreResult.getReferenceId())
                .submittedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PersonalLoanApplication saved = applicationPort.save(application);

        List<AmortizationInstallment> schedule = schedulePort.saveAll(
                applicationId,
                simulation.getSchedule()
        );

        return mapper.toResponse(saved, schedule);
    }
}
