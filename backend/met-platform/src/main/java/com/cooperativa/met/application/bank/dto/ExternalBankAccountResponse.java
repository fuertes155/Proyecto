package com.cooperativa.met.application.bank.dto;

import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ExternalBankAccountResponse(
        UUID id,
        String bankCode,
        String bankName,
        BankAccountType accountType,
        String maskedAccountNumber,
        BankAccountVerificationStatus verificationStatus,
        boolean verificationPending,
        Instant createdAt
) {
}
