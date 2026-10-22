package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayRequest;
import com.cooperativa.met.domain.bank.port.PayoutGatewayResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Prueba de titularidad de una cuenta bancaria externa: envío del micro-depósito y su confirmación. */
@ExtendWith(MockitoExtension.class)
class BankAccountVerificationUseCasesTest {

    @Mock private ExternalBankAccountRepositoryPort externalBankAccountRepository;
    @Mock private BankRepositoryPort bankRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private PayoutGatewayPort payoutGatewayPort;
    @Mock private AuditLogService auditLogService;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @Nested
    class SendingTheMicroDeposit {

        private InitiateBankAccountVerificationUseCase initiateUseCase;

        @BeforeEach
        void setUp() {
            initiateUseCase = new InitiateBankAccountVerificationUseCase(externalBankAccountRepository, bankRepository,
                    userRepository, payoutGatewayPort);
        }

        @Test
        void shouldSendARandomSmallAmountAndRememberIt() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, null, 0));
            when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1507")));
            givenUser("socio@example.com");
            when(payoutGatewayPort.initiatePayout(any()))
                    .thenReturn(new PayoutGatewayResult("rail-1", PayoutGatewayResult.PayoutGatewayStatus.ACCEPTED, null, null));

            initiateUseCase.execute(userId, accountId);

            ArgumentCaptor<PayoutGatewayRequest> sent = ArgumentCaptor.forClass(PayoutGatewayRequest.class);
            verify(payoutGatewayPort).initiatePayout(sent.capture());
            int amount = sent.getValue().amount().intValueExact();
            assertTrue(amount >= 1000 && amount <= 4900, "monto fuera de rango: " + amount);
            assertTrue(sent.getValue().reference().startsWith("VERIFY-" + accountId));
            assertEquals("00123456789", sent.getValue().destinationAccountNumber());
            assertEquals("1086578123", sent.getValue().beneficiaryDocumentNumber());

            assertEquals(amount, savedAccount().getPendingVerificationAmount());
        }

        @Test
        void shouldNotRememberAnAmountThatWasNeverSent() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, null, 0));
            when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1507")));
            givenUser("socio@example.com");
            when(payoutGatewayPort.initiatePayout(any())).thenReturn(new PayoutGatewayResult(null,
                    PayoutGatewayResult.PayoutGatewayStatus.REJECTED, "DOWN", "Proveedor caído"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> initiateUseCase.execute(userId, accountId));

            assertEquals("VERIFICATION_SEND_FAILED", ex.getCode());
            verify(externalBankAccountRepository, never()).save(any());
        }

        @Test
        void shouldRefuseAccountOfAnotherUser() {
            givenAccount(account(UUID.randomUUID(), BankAccountVerificationStatus.PENDING, null, 0));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> initiateUseCase.execute(userId, accountId));

            assertEquals("FORBIDDEN", ex.getCode());
            verifyNoInteractions(payoutGatewayPort);
        }

        @Test
        void shouldNotResendToAnAlreadyVerifiedAccount() {
            givenAccount(account(userId, BankAccountVerificationStatus.VERIFIED, null, 0));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> initiateUseCase.execute(userId, accountId));

            assertEquals("ALREADY_VERIFIED", ex.getCode());
        }

        @Test
        void shouldRefuseBankNotYetAvailableOnTheRail() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, null, 0));
            when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank(" ")));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> initiateUseCase.execute(userId, accountId));

            assertEquals("BANK_NOT_SYNCED", ex.getCode());
            verifyNoInteractions(payoutGatewayPort);
        }

        @Test
        void shouldAskForAnEmailBeforeSending() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, null, 0));
            when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1507")));
            givenUser(null);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> initiateUseCase.execute(userId, accountId));

            assertEquals("EMAIL_REQUIRED", ex.getCode());
            verifyNoInteractions(payoutGatewayPort);
        }
    }

    @Nested
    class ConfirmingTheAmount {

        private ConfirmBankAccountVerificationUseCase confirmUseCase;

        @BeforeEach
        void setUp() {
            confirmUseCase = new ConfirmBankAccountVerificationUseCase(externalBankAccountRepository, auditLogService);
        }

        @Test
        void shouldVerifyAccountWhenTheAmountMatches() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, 2731, 2));

            confirmUseCase.execute(userId, accountId, 2731);

            ExternalBankAccount saved = savedAccount();
            assertEquals(BankAccountVerificationStatus.VERIFIED, saved.getVerificationStatus());
            assertNull(saved.getPendingVerificationAmount());
            verify(auditLogService).logSuccess(userId, "EXTERNAL_BANK_ACCOUNT_VERIFIED", "EXTERNAL_BANK_ACCOUNT", accountId.toString(), "{}");
        }

        @Test
        void shouldCountAWrongGuess() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, 2731, 1));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 2730));

            assertEquals("INCORRECT_AMOUNT", ex.getCode());
            assertEquals(2, savedAccount().getVerificationAttempts());
            assertEquals(2731, savedAccount().getPendingVerificationAmount());
        }

        @Test
        void shouldDiscardThePendingAmountOnTheFifthWrongGuess() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, 2731, 4));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 2730));

            assertEquals("MAX_ATTEMPTS_EXCEEDED", ex.getCode());
            assertNull(savedAccount().getPendingVerificationAmount());
        }

        @Test
        void shouldNotAcceptEvenTheRightAmountOnceAttemptsAreExhausted() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, 2731, 5));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 2731));

            assertEquals("MAX_ATTEMPTS_EXCEEDED", ex.getCode());
            assertEquals(BankAccountVerificationStatus.PENDING, savedAccount().getVerificationStatus());
        }

        @Test
        void shouldAuditAttemptsToVerifySomeoneElsesAccount() {
            givenAccount(account(UUID.randomUUID(), BankAccountVerificationStatus.PENDING, 2731, 0));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 2731));

            assertEquals("FORBIDDEN", ex.getCode());
            verify(auditLogService).logFailure(eq(userId), eq("EXTERNAL_BANK_ACCOUNT_VERIFY_FORBIDDEN"),
                    eq("EXTERNAL_BANK_ACCOUNT"), eq(accountId.toString()), anyString());
            verify(externalBankAccountRepository, never()).save(any());
        }

        @Test
        void shouldAskForANewDepositWhenNoneIsPending() {
            givenAccount(account(userId, BankAccountVerificationStatus.PENDING, null, 0));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 2731));

            assertEquals("NO_VERIFICATION_PENDING", ex.getCode());
        }

        @Test
        void shouldRefuseToVerifyTwice() {
            givenAccount(account(userId, BankAccountVerificationStatus.VERIFIED, null, 0));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> confirmUseCase.execute(userId, accountId, 2731));

            assertEquals("ALREADY_VERIFIED", ex.getCode());
        }
    }

    private void givenAccount(ExternalBankAccount account) {
        when(externalBankAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
    }

    private void givenUser(String email) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId).documentType(DocumentType.CC).documentNumber("1086578123")
                .firstName("Ana").lastName("Pérez").email(email).build()));
    }

    private ExternalBankAccount account(UUID owner, BankAccountVerificationStatus status, Integer pendingAmount, int attempts) {
        return ExternalBankAccount.builder()
                .id(accountId)
                .userId(owner)
                .bankCode("1007")
                .accountType(BankAccountType.SAVINGS)
                .accountNumber("00123456789")
                .verificationStatus(status)
                .pendingVerificationAmount(pendingAmount)
                .verificationAttempts(attempts)
                .active(true)
                .build();
    }

    private Bank bank(String wompiBankId) {
        return Bank.builder().code("1007").name("Bancolombia").wompiBankId(wompiBankId).supportsPayout(true).active(true).build();
    }

    private ExternalBankAccount savedAccount() {
        ArgumentCaptor<ExternalBankAccount> captor = ArgumentCaptor.forClass(ExternalBankAccount.class);
        verify(externalBankAccountRepository).save(captor.capture());
        return captor.getValue();
    }
}
