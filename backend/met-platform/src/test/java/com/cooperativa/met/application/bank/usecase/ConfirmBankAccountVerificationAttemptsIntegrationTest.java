package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Con base de datos real (H2): cada intento fallido guarda el contador y enseguida lanza la
 * excepción. Si ese guardado se revirtiera, el límite de 5 intentos nunca se alcanzaría y el
 * monto del micro-depósito se podría adivinar por fuerza bruta.
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class ConfirmBankAccountVerificationAttemptsIntegrationTest {

    @Autowired
    private ConfirmBankAccountVerificationUseCase confirmUseCase;
    @Autowired
    private ExternalBankAccountRepositoryPort externalBankAccountRepository;

    @Test
    void shouldCountEveryWrongGuessAndLockAfterTheFifth() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        externalBankAccountRepository.save(ExternalBankAccount.builder()
                .id(accountId)
                .userId(userId)
                .bankCode("1007")
                .accountType(BankAccountType.SAVINGS)
                .accountNumber("00123456789")
                .verificationStatus(BankAccountVerificationStatus.PENDING)
                .pendingVerificationAmount(2731)
                .verificationAttempts(0)
                .active(true)
                .createdAt(Instant.now())
                .build());

        BusinessRuleException first = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 1000));
        assertEquals("INCORRECT_AMOUNT", first.getCode());
        assertEquals(1, externalBankAccountRepository.findById(accountId).orElseThrow().getVerificationAttempts(),
                "el intento fallido debe quedar contado aunque el caso de uso termine en error");

        for (int guess = 1001; guess <= 1003; guess++) {
            int amount = guess;
            assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, amount));
        }
        BusinessRuleException fifth = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 1004));
        assertEquals("MAX_ATTEMPTS_EXCEEDED", fifth.getCode());

        // Agotados los intentos, ni siquiera el monto correcto sirve: hay que pedir otro depósito.
        BusinessRuleException afterLock = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 2731));
        assertEquals("NO_VERIFICATION_PENDING", afterLock.getCode());
        assertEquals(BankAccountVerificationStatus.PENDING,
                externalBankAccountRepository.findById(accountId).orElseThrow().getVerificationStatus());
    }
}
