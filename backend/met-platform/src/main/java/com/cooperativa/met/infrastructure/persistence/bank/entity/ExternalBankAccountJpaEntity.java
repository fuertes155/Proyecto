package com.cooperativa.met.infrastructure.persistence.bank.entity;

import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.infrastructure.security.converter.StandardCryptoConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalBankAccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private BankAccountType accountType;

    @Convert(converter = StandardCryptoConverter.class)
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private BankAccountVerificationStatus verificationStatus;

    @Convert(converter = StandardCryptoConverter.class)
    @Column(name = "pending_verification_amount")
    private String pendingVerificationAmount;

    @Column(name = "verification_attempts", nullable = false)
    private int verificationAttempts;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public ExternalBankAccount toDomain() {
        return ExternalBankAccount.builder()
                .id(id)
                .userId(userId)
                .bankCode(bankCode)
                .accountType(accountType)
                .accountNumber(accountNumber)
                .verificationStatus(verificationStatus)
                .pendingVerificationAmount(pendingVerificationAmount == null ? null : Integer.valueOf(pendingVerificationAmount))
                .verificationAttempts(verificationAttempts)
                .active(active)
                .createdAt(createdAt)
                .verifiedAt(verifiedAt)
                .build();
    }

    public static ExternalBankAccountJpaEntity fromDomain(ExternalBankAccount domain) {
        return ExternalBankAccountJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .bankCode(domain.getBankCode())
                .accountType(domain.getAccountType())
                .accountNumber(domain.getAccountNumber())
                .verificationStatus(domain.getVerificationStatus())
                .pendingVerificationAmount(domain.getPendingVerificationAmount() == null ? null : domain.getPendingVerificationAmount().toString())
                .verificationAttempts(domain.getVerificationAttempts())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .verifiedAt(domain.getVerifiedAt())
                .build();
    }
}
