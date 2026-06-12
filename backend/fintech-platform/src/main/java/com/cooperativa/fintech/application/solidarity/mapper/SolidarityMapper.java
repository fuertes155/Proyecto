package com.cooperativa.fintech.application.solidarity.mapper;

import com.cooperativa.fintech.application.solidarity.dto.LoanInstallmentResponse;
import com.cooperativa.fintech.application.solidarity.dto.MicroLoanResponse;
import com.cooperativa.fintech.application.solidarity.dto.PoolTransactionResponse;
import com.cooperativa.fintech.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.fintech.application.solidarity.dto.SolidarityMemberResponse;
import com.cooperativa.fintech.domain.solidarity.model.LoanInstallment;
import com.cooperativa.fintech.domain.solidarity.model.MemberRole;
import com.cooperativa.fintech.domain.solidarity.model.MicroLoan;
import com.cooperativa.fintech.domain.solidarity.model.PoolTransaction;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityMember;
import org.springframework.stereotype.Component;

@Component
public class SolidarityMapper {

    public SolidarityGroupResponse toResponse(SolidarityGroup group, int memberCount, MemberRole myRole) {
        return new SolidarityGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getInviteCode(),
                group.getMinContribution(),
                group.getMaxLoanPercentage(),
                group.getInterestRate(),
                group.getPoolBalance(),
                group.maxLoanAmount(),
                memberCount,
                group.getMaxMembers(),
                group.getStatus(),
                myRole,
                group.getCreatedAt()
        );
    }

    public SolidarityMemberResponse toResponse(SolidarityMember member) {
        return new SolidarityMemberResponse(
                member.getId(),
                member.getUserId(),
                member.getRole(),
                member.getTotalContributed(),
                member.getJoinedAt()
        );
    }

    public MicroLoanResponse toResponse(MicroLoan loan) {
        return new MicroLoanResponse(
                loan.getId(),
                loan.getGroupId(),
                loan.getBorrowerId(),
                loan.getAmount(),
                loan.getPurpose(),
                loan.getTermMonths(),
                loan.getInterestRate(),
                loan.getStatus(),
                loan.getRequestedAt(),
                loan.getReviewedAt(),
                loan.getDisbursedAt(),
                loan.getRejectionReason()
        );
    }

    public LoanInstallmentResponse toResponse(LoanInstallment installment) {
        return new LoanInstallmentResponse(
                installment.getId(),
                installment.getLoanId(),
                installment.getInstallmentNumber(),
                installment.getPrincipalAmount(),
                installment.getInterestAmount(),
                installment.getTotalAmount(),
                installment.getDueDate(),
                installment.getPaidAt(),
                installment.getStatus()
        );
    }

    public PoolTransactionResponse toResponse(PoolTransaction transaction) {
        return new PoolTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
