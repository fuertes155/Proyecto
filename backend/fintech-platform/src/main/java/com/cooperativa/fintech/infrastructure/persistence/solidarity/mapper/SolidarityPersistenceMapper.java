package com.cooperativa.fintech.infrastructure.persistence.solidarity.mapper;

import com.cooperativa.fintech.domain.solidarity.model.LoanInstallment;
import com.cooperativa.fintech.domain.solidarity.model.MicroLoan;
import com.cooperativa.fintech.domain.solidarity.model.PoolTransaction;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityMember;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.entity.LoanInstallmentJpaEntity;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.entity.MicroLoanJpaEntity;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.entity.PoolTransactionJpaEntity;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.entity.SolidarityGroupJpaEntity;
import com.cooperativa.fintech.infrastructure.persistence.solidarity.entity.SolidarityMemberJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SolidarityPersistenceMapper {

    public SolidarityGroup toDomain(SolidarityGroupJpaEntity e) {
        return SolidarityGroup.builder()
                .id(e.getId()).name(e.getName()).description(e.getDescription())
                .creatorId(e.getCreatorId()).inviteCode(e.getInviteCode())
                .minContribution(e.getMinContribution()).maxLoanPercentage(e.getMaxLoanPercentage())
                .interestRate(e.getInterestRate()).poolBalance(e.getPoolBalance())
                .maxMembers(e.getMaxMembers()).status(e.getStatus())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public SolidarityGroupJpaEntity toEntity(SolidarityGroup g) {
        SolidarityGroupJpaEntity e = new SolidarityGroupJpaEntity();
        e.setId(g.getId()); e.setName(g.getName()); e.setDescription(g.getDescription());
        e.setCreatorId(g.getCreatorId()); e.setInviteCode(g.getInviteCode());
        e.setMinContribution(g.getMinContribution()); e.setMaxLoanPercentage(g.getMaxLoanPercentage());
        e.setInterestRate(g.getInterestRate()); e.setPoolBalance(g.getPoolBalance());
        e.setMaxMembers(g.getMaxMembers()); e.setStatus(g.getStatus());
        e.setCreatedAt(g.getCreatedAt()); e.setUpdatedAt(g.getUpdatedAt());
        return e;
    }

    public SolidarityMember toDomain(SolidarityMemberJpaEntity e) {
        return SolidarityMember.builder()
                .id(e.getId()).groupId(e.getGroupId()).userId(e.getUserId())
                .role(e.getRole()).totalContributed(e.getTotalContributed())
                .joinedAt(e.getJoinedAt()).build();
    }

    public SolidarityMemberJpaEntity toEntity(SolidarityMember m) {
        SolidarityMemberJpaEntity e = new SolidarityMemberJpaEntity();
        e.setId(m.getId()); e.setGroupId(m.getGroupId()); e.setUserId(m.getUserId());
        e.setRole(m.getRole()); e.setTotalContributed(m.getTotalContributed());
        e.setJoinedAt(m.getJoinedAt());
        return e;
    }

    public MicroLoan toDomain(MicroLoanJpaEntity e) {
        return MicroLoan.builder()
                .id(e.getId()).groupId(e.getGroupId()).borrowerId(e.getBorrowerId())
                .amount(e.getAmount()).purpose(e.getPurpose()).termMonths(e.getTermMonths())
                .interestRate(e.getInterestRate()).status(e.getStatus())
                .requestedAt(e.getRequestedAt()).reviewedAt(e.getReviewedAt())
                .reviewedBy(e.getReviewedBy()).disbursedAt(e.getDisbursedAt())
                .rejectionReason(e.getRejectionReason()).build();
    }

    public MicroLoanJpaEntity toEntity(MicroLoan l) {
        MicroLoanJpaEntity e = new MicroLoanJpaEntity();
        e.setId(l.getId()); e.setGroupId(l.getGroupId()); e.setBorrowerId(l.getBorrowerId());
        e.setAmount(l.getAmount()); e.setPurpose(l.getPurpose()); e.setTermMonths(l.getTermMonths());
        e.setInterestRate(l.getInterestRate()); e.setStatus(l.getStatus());
        e.setRequestedAt(l.getRequestedAt()); e.setReviewedAt(l.getReviewedAt());
        e.setReviewedBy(l.getReviewedBy()); e.setDisbursedAt(l.getDisbursedAt());
        e.setRejectionReason(l.getRejectionReason());
        return e;
    }

    public LoanInstallment toDomain(LoanInstallmentJpaEntity e) {
        return LoanInstallment.builder()
                .id(e.getId()).loanId(e.getLoanId()).installmentNumber(e.getInstallmentNumber())
                .principalAmount(e.getPrincipalAmount()).interestAmount(e.getInterestAmount())
                .totalAmount(e.getTotalAmount()).dueDate(e.getDueDate())
                .paidAt(e.getPaidAt()).status(e.getStatus()).build();
    }

    public LoanInstallmentJpaEntity toEntity(LoanInstallment i) {
        LoanInstallmentJpaEntity e = new LoanInstallmentJpaEntity();
        e.setId(i.getId()); e.setLoanId(i.getLoanId()); e.setInstallmentNumber(i.getInstallmentNumber());
        e.setPrincipalAmount(i.getPrincipalAmount()); e.setInterestAmount(i.getInterestAmount());
        e.setTotalAmount(i.getTotalAmount()); e.setDueDate(i.getDueDate());
        e.setPaidAt(i.getPaidAt()); e.setStatus(i.getStatus());
        return e;
    }

    public PoolTransaction toDomain(PoolTransactionJpaEntity e) {
        return PoolTransaction.builder()
                .id(e.getId()).groupId(e.getGroupId()).memberId(e.getMemberId())
                .loanId(e.getLoanId()).type(e.getType()).amount(e.getAmount())
                .balanceAfter(e.getBalanceAfter()).description(e.getDescription())
                .createdAt(e.getCreatedAt()).build();
    }

    public PoolTransactionJpaEntity toEntity(PoolTransaction t) {
        PoolTransactionJpaEntity e = new PoolTransactionJpaEntity();
        e.setId(t.getId()); e.setGroupId(t.getGroupId()); e.setMemberId(t.getMemberId());
        e.setLoanId(t.getLoanId()); e.setType(t.getType()); e.setAmount(t.getAmount());
        e.setBalanceAfter(t.getBalanceAfter()); e.setDescription(t.getDescription());
        e.setCreatedAt(t.getCreatedAt());
        return e;
    }
}
