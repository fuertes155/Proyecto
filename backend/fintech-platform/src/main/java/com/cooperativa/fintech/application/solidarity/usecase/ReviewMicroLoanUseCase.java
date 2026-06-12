package com.cooperativa.fintech.application.solidarity.usecase;

import com.cooperativa.fintech.application.solidarity.dto.MicroLoanResponse;
import com.cooperativa.fintech.application.solidarity.dto.ReviewMicroLoanRequest;
import com.cooperativa.fintech.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.fintech.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.solidarity.model.MicroLoan;
import com.cooperativa.fintech.domain.solidarity.model.MicroLoanStatus;
import com.cooperativa.fintech.domain.solidarity.model.PoolTransaction;
import com.cooperativa.fintech.domain.solidarity.model.PoolTransactionType;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.fintech.domain.solidarity.port.LoanInstallmentPort;
import com.cooperativa.fintech.domain.solidarity.port.MicroLoanPort;
import com.cooperativa.fintech.domain.solidarity.port.PoolTransactionPort;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityPoolCachePort;
import com.cooperativa.fintech.domain.solidarity.service.InstallmentPlanCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewMicroLoanUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final MicroLoanPort loanPort;
    private final SolidarityGroupPort groupPort;
    private final LoanInstallmentPort installmentPort;
    private final PoolTransactionPort transactionPort;
    private final SolidarityPoolCachePort poolCachePort;
    private final SolidarityMapper mapper;

    @Transactional
    public MicroLoanResponse execute(UUID userId, UUID groupId, UUID loanId, ReviewMicroLoanRequest request) {
        authorizationService.requireAdmin(groupId, userId);

        MicroLoan loan = loanPort.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Micropréstamo no encontrado"));

        if (!loan.getGroupId().equals(groupId)) {
            throw new BusinessRuleException("LOAN_GROUP_MISMATCH", "El préstamo no pertenece a este grupo");
        }

        if (loan.getStatus() != MicroLoanStatus.PENDING) {
            throw new BusinessRuleException("LOAN_NOT_PENDING", "El préstamo ya fue procesado");
        }

        if (!request.approved()) {
            MicroLoan rejected = loanPort.save(loan.reject(userId, request.rejectionReason()));
            return mapper.toResponse(rejected);
        }

        SolidarityGroup group = authorizationService.requireGroup(groupId);
        if (loan.getAmount().compareTo(group.getPoolBalance()) > 0) {
            throw new BusinessRuleException("INSUFFICIENT_POOL", "Fondos insuficientes en el fondo solidario");
        }

        var newBalance = group.getPoolBalance().subtract(loan.getAmount());
        SolidarityGroup updatedGroup = groupPort.save(group.withPoolBalance(newBalance));

        MicroLoan disbursed = loan.approve(userId).disburse();
        MicroLoan savedLoan = loanPort.save(disbursed);

        installmentPort.saveAll(InstallmentPlanCalculator.generate(
                savedLoan.getId(),
                savedLoan.getAmount(),
                savedLoan.getInterestRate(),
                savedLoan.getTermMonths(),
                LocalDate.now()
        ));

        transactionPort.save(PoolTransaction.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .loanId(savedLoan.getId())
                .type(PoolTransactionType.LOAN_DISBURSEMENT)
                .amount(savedLoan.getAmount())
                .balanceAfter(newBalance)
                .description("Desembolso micropréstamo")
                .createdAt(Instant.now())
                .build());

        poolCachePort.invalidate(groupId);
        poolCachePort.cacheBalance(groupId, newBalance);

        return mapper.toResponse(savedLoan);
    }
}
