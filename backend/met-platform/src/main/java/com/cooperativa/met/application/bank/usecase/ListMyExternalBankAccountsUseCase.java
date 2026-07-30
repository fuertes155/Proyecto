package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.bank.dto.ExternalBankAccountResponse;
import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListMyExternalBankAccountsUseCase {

    private final ExternalBankAccountRepositoryPort externalBankAccountRepository;
    private final BankRepositoryPort bankRepository;

    public List<ExternalBankAccountResponse> execute(UUID userId) {
        List<ExternalBankAccount> accounts = externalBankAccountRepository.findActiveByUserId(userId);

        Map<String, Bank> banksByCode = bankRepository.findAllActive().stream()
                .collect(Collectors.toMap(Bank::getCode, Function.identity(), (a, b) -> a));

        return accounts.stream()
                .map(account -> ExternalBankAccountResponse.builder()
                        .id(account.getId())
                        .bankCode(account.getBankCode())
                        .bankName(banksByCode.containsKey(account.getBankCode())
                                ? banksByCode.get(account.getBankCode()).getName()
                                : account.getBankCode())
                        .accountType(account.getAccountType())
                        .maskedAccountNumber(mask(account.getAccountNumber()))
                        .verificationStatus(account.getVerificationStatus())
                        .verificationPending(account.getPendingVerificationAmount() != null)
                        .createdAt(account.getCreatedAt())
                        .build())
                .toList();
    }

    private String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
