package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.met.application.lending.dto.SubmitLoanApplicationRequest;
import com.cooperativa.met.application.lending.mapper.LendingMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.KycStatus;
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
import com.cooperativa.met.domain.lending.model.LoanEligibilityDecision;
import com.cooperativa.met.domain.lending.service.CreditScoringEngine;
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
    private final CreditBureauPort creditBureauPort;
    private final LendingMapper mapper;

    @Transactional
    public LoanApplicationResponse execute(UUID userId, SubmitLoanApplicationRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Usuario no encontrado"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "Cuenta no activa");
        }

        if (user.getKycStatus() != KycStatus.APPROVED) {
            throw new BusinessRuleException("KYC_REQUIRED", "Debe completar la validación de identidad biométrica para solicitar un préstamo");
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

        // Motor de Scoring: si el score ya descalifica, no hace falta consultar el saldo
        LoanEligibilityDecision preliminary = CreditScoringEngine.evaluate(scoreResult.getScore(), BigDecimal.ZERO);
        if (!preliminary.isApproved()) {
            throw new BusinessRuleException("RISK_SCORE_LOW", preliminary.getReason());
        }

        // Capa 4: Validación de Saldo (Ahorro)
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("NO_ACCOUNT", "El usuario no tiene billetera virtual para validar el saldo"));

        // Única fuente de verdad para monto máximo, plazo y tasa según banda de riesgo
        LoanEligibilityDecision eligibility = CreditScoringEngine.evaluate(scoreResult.getScore(), account.getPrincipalBalance());

        if (request.amount().compareTo(eligibility.getMaxAmount()) > 0) {
            throw new BusinessRuleException("MAX_LOAN_EXCEEDED",
                    "El monto solicitado excede tu límite para tu perfil de riesgo (" + eligibility.getTier()
                            + "). Máximo disponible: $" + eligibility.getMaxAmount());
        }

        if (request.termMonths() > eligibility.getMaxTermMonths()) {
            throw new BusinessRuleException("MAX_TERM_EXCEEDED",
                    "El plazo solicitado excede el máximo permitido para tu perfil de riesgo ("
                            + eligibility.getMaxTermMonths() + " meses).");
        }

        LoanSimulationResult simulation = FrenchAmortizationCalculator.simulate(
                request.amount(),
                request.termMonths(),
                eligibility.getAnnualInterestRate(),
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
                .riskTier(eligibility.getTier())
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
