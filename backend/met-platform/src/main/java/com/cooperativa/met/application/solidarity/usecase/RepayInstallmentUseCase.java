package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.LoanInstallmentResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;
import com.cooperativa.met.domain.solidarity.model.InstallmentStatus;
import com.cooperativa.met.domain.solidarity.model.LoanInstallment;
import com.cooperativa.met.domain.solidarity.model.MicroLoan;
import com.cooperativa.met.domain.solidarity.model.MicroLoanStatus;
import com.cooperativa.met.domain.solidarity.model.PoolTransaction;
import com.cooperativa.met.domain.solidarity.model.PoolTransactionType;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.port.LoanInstallmentPort;
import com.cooperativa.met.domain.solidarity.port.MicroLoanPort;
import com.cooperativa.met.domain.solidarity.port.PoolTransactionPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityPoolCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepayInstallmentUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final LoanInstallmentPort installmentPort;
    private final MicroLoanPort loanPort;
    private final SolidarityGroupPort groupPort;
    private final PoolTransactionPort transactionPort;
    private final DebitSourcePort debitSourcePort;
    private final SolidarityPoolCachePort poolCachePort;
    private final SolidarityMapper mapper;

    @Transactional
    public LoanInstallmentResponse execute(UUID userId, UUID groupId, UUID loanId, UUID installmentId) {
        authorizationService.requireMembership(groupId, userId);

        MicroLoan loan = loanPort.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Micropréstamo no encontrado"));

        if (!loan.getBorrowerId().equals(userId)) {
            throw new BusinessRuleException("NOT_BORROWER", "Solo el prestatario puede realizar el pago");
        }

        if (loan.getStatus() != MicroLoanStatus.DISBURSED) {
            throw new BusinessRuleException("LOAN_NOT_DISBURSED", "El préstamo no está en estado de pago");
        }

        LoanInstallment installment = installmentPort.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));

        if (!installment.getLoanId().equals(loanId)) {
            throw new BusinessRuleException("INSTALLMENT_MISMATCH", "La cuota no pertenece a este préstamo");
        }

        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new BusinessRuleException("ALREADY_PAID", "La cuota ya fue pagada");
        }

        String reference = "SOLID-REPAY-" + installmentId;
        if (!debitSourcePort.debit(userId, installment.getTotalAmount(), reference)) {
            throw new BusinessRuleException("INSUFFICIENT_FUNDS", "Fondos insuficientes para el pago");
        }

        LoanInstallment paid = installmentPort.save(installment.markPaid());

        SolidarityGroup group = authorizationService.requireGroup(groupId);
        var newBalance = group.getPoolBalance().add(installment.getTotalAmount());
        groupPort.save(group.withPoolBalance(newBalance));

        transactionPort.save(PoolTransaction.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .loanId(loanId)
                .type(PoolTransactionType.LOAN_REPAYMENT)
                .amount(installment.getTotalAmount())
                .balanceAfter(newBalance)
                .description("Pago cuota #" + installment.getInstallmentNumber())
                .createdAt(Instant.now())
                .build());

        if (installmentPort.allPaid(loanId)) {
            loanPort.save(loan.markRepaid());
        }

        poolCachePort.invalidate(groupId);
        poolCachePort.cacheBalance(groupId, newBalance);

        return mapper.toResponse(paid);
    }
}
