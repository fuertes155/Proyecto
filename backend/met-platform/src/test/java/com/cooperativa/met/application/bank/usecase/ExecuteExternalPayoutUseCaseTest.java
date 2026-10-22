package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.account.service.AccountReversalService;
import com.cooperativa.met.application.bank.dto.ExecutePayoutRequest;
import com.cooperativa.met.application.bank.service.PayoutLimitService;
import com.cooperativa.met.application.compliance.service.SarlaftAlertEngine;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.application.security.FraudDetectionService;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.model.ExternalPayout;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalPayoutRepositoryPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayRequest;
import com.cooperativa.met.domain.bank.port.PayoutGatewayResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import com.cooperativa.met.infrastructure.security.IdempotencyService;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecuteExternalPayoutUseCaseTest {

    private static final String IP = "190.85.10.4";
    private static final String DOCUMENT = "1086578123";
    private static final BigDecimal AMOUNT = new BigDecimal("150000");

    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private CoreTransactionRepositoryPort transactionRepository;
    @Mock private ExternalBankAccountRepositoryPort externalBankAccountRepository;
    @Mock private ExternalPayoutRepositoryPort externalPayoutRepository;
    @Mock private BankRepositoryPort bankRepository;
    @Mock private PayoutGatewayPort payoutGatewayPort;
    @Mock private PayoutLimitService payoutLimitService;
    @Mock private AccountReversalService accountReversalService;
    @Mock private UserRepositoryPort userRepository;
    @Mock private EncryptionPort encryptionPort;
    @Mock private OtpService otpService;
    @Mock private AuditLogService auditLogService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private PinAttemptService pinAttemptService;
    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private SarlaftAlertEngine sarlaftAlertEngine;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private ExecuteExternalPayoutUseCase payoutUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID externalAccountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        payoutUseCase.initTransactionTemplate();
    }

    @Test
    void shouldDebitEarningsAndSendPayoutToTheRail() {
        givenValidRequestUpToTheDebit(new BigDecimal("400000"));
        when(payoutGatewayPort.initiatePayout(any()))
                .thenReturn(new PayoutGatewayResult("wompi-tx-77", PayoutGatewayResult.PayoutGatewayStatus.ACCEPTED, null, null));

        payoutUseCase.execute(userId, request("idem-1"), IP);

        ArgumentCaptor<CoreAccount> debited = ArgumentCaptor.forClass(CoreAccount.class);
        verify(accountRepository).save(debited.capture());
        assertEquals(new BigDecimal("250000"), debited.getValue().getInterestBalance());

        CoreTransaction pending = savedTransactions().get(0);
        assertEquals(TransactionType.EXTERNAL_PAYOUT, pending.getType());
        assertEquals(TransactionStatus.PENDING, pending.getStatus());
        assertEquals(AMOUNT, pending.getAmount());
        verify(sarlaftAlertEngine).evaluate(userId, sourceAccountId, pending.getId(), TransactionType.EXTERNAL_PAYOUT, AMOUNT);

        PayoutGatewayRequest sent = gatewayRequest();
        assertEquals("1507", sent.destinationWompiBankId());
        assertEquals("SAVINGS", sent.destinationAccountType());
        assertEquals("00123456789", sent.destinationAccountNumber());
        assertEquals(DOCUMENT, sent.beneficiaryDocumentNumber());
        assertEquals("Ana Pérez", sent.beneficiaryFullName());
        assertEquals("PAYOUT-" + sent.internalPayoutId(), sent.reference());

        List<ExternalPayout> payouts = savedPayouts();
        assertEquals("wompi-tx-77", payouts.get(payouts.size() - 1).getRailReference());
        verify(accountReversalService, never()).creditWithRetry(any(), any());
        verify(auditLogService).logSuccess(eq(userId), eq("EXTERNAL_PAYOUT_INITIATED"), eq("EXTERNAL_PAYOUT"),
                eq(pending.getId().toString()), contains("\"amount\":\"***\""));
        verify(idempotencyService).saveResult("idem-1", pending.getId().toString());
    }

    @Test
    void shouldGiveTheMoneyBackWhenTheRailRejectsThePayout() {
        givenValidRequestUpToTheDebit(new BigDecimal("400000"));
        when(payoutGatewayPort.initiatePayout(any())).thenReturn(new PayoutGatewayResult(null,
                PayoutGatewayResult.PayoutGatewayStatus.REJECTED, "ACCOUNT_CLOSED", "La cuenta destino está cerrada"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("ACCOUNT_CLOSED", ex.getCode());
        verify(accountReversalService).creditWithRetry(sourceAccountId, AMOUNT);
        List<CoreTransaction> transactions = savedTransactions();
        assertEquals(TransactionStatus.FAILED, transactions.get(transactions.size() - 1).getStatus());
        List<ExternalPayout> payouts = savedPayouts();
        assertEquals("ACCOUNT_CLOSED", payouts.get(payouts.size() - 1).getFailureCode());
        verify(auditLogService).logFailure(eq(userId), eq("EXTERNAL_PAYOUT_FAILED"), eq("EXTERNAL_PAYOUT"), anyString(), anyString());
        verify(idempotencyService, never()).saveResult(anyString(), anyString());
        verify(idempotencyService, never()).release(anyString());
    }

    @Test
    void shouldGiveTheMoneyBackWhenTheRailCannotBeReached() {
        givenValidRequestUpToTheDebit(new BigDecimal("400000"));
        when(payoutGatewayPort.initiatePayout(any())).thenThrow(new IllegalStateException("timeout"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("GATEWAY_ERROR", ex.getCode());
        verify(accountReversalService).creditWithRetry(sourceAccountId, AMOUNT);
    }

    @Test
    void shouldOnlyAllowWithdrawingEarningsNotCapital() {
        givenValidRequestUpToTheDebit(new BigDecimal("100000"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("INSUFFICIENT_EARNINGS", ex.getCode());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(payoutGatewayPort);
        verify(idempotencyService).release("idem-1");
    }

    @Test
    void shouldFreeTheIdempotencyKeyWhenTheOtpIsStillMissing() {
        // La app envía primero sin código, recibe OTP_REQUIRED y reenvía con el código y la misma
        // clave: si la clave quedara tomada, el segundo envío respondería CONCURRENT_REQUEST.
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        givenValidPin();
        givenOwnedBankAccountAndSyncedBank();
        ExecutePayoutRequest withoutOtp =
                new ExecutePayoutRequest(externalAccountId, AMOUNT, "Retiro de ganancias", "rsa(pin)", null, "idem-1");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, withoutOtp, IP));

        assertEquals("OTP_REQUIRED", ex.getCode());
        verify(idempotencyService).release("idem-1");
    }

    @Test
    void shouldRefuseToPayIntoSomeoneElsesBankAccount() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        givenValidPin();
        when(externalBankAccountRepository.findById(externalAccountId))
                .thenReturn(Optional.of(externalAccount(UUID.randomUUID(), true, BankAccountVerificationStatus.VERIFIED)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("FORBIDDEN_DESTINATION", ex.getCode());
        verify(auditLogService).logFailure(eq(userId), eq("EXTERNAL_PAYOUT_FORBIDDEN_DESTINATION"), eq("EXTERNAL_BANK_ACCOUNT"),
                eq(externalAccountId.toString()), anyString());
        verifyNoInteractions(accountRepository, payoutGatewayPort, otpService);
    }

    @Test
    void shouldStopOnWrongPin() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        when(encryptionPort.decryptRsa("rsa(pin)")).thenReturn("0000");
        when(encryptionPort.verifyPin("0000", "pin-hash")).thenReturn(false);
        doThrow(new BusinessRuleException("INVALID_PIN", "PIN incorrecto")).when(pinAttemptService).checkAndRecordFailure(userId);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("INVALID_PIN", ex.getCode());
        verify(pinAttemptService, never()).resetAttempts(any());
        verifyNoInteractions(accountRepository, payoutGatewayPort);
    }

    @Test
    void shouldTreatUndecryptablePinAsWrongPin() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        when(encryptionPort.decryptRsa("rsa(pin)")).thenThrow(new IllegalArgumentException("padding"));
        doThrow(new BusinessRuleException("INVALID_PIN", "PIN incorrecto")).when(pinAttemptService).checkAndRecordFailure(userId);

        assertThrows(BusinessRuleException.class, () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        verify(pinAttemptService).checkAndRecordFailure(userId);
    }

    @Test
    void shouldStopOnWrongOtpBeforeMovingMoney() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        givenValidPin();
        givenOwnedBankAccountAndSyncedBank();
        when(otpService.validateOtp(DOCUMENT, "482913")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("INVALID_OTP", ex.getCode());
        verifyNoInteractions(accountRepository, payoutGatewayPort);
    }

    @Test
    void shouldRequireOtp() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        givenValidPin();
        givenOwnedBankAccountAndSyncedBank();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> payoutUseCase.execute(userId,
                new ExecutePayoutRequest(externalAccountId, AMOUNT, "Retiro", "rsa(pin)", "", "idem-1"), IP));

        assertEquals("OTP_REQUIRED", ex.getCode());
        verifyNoInteractions(otpService, accountRepository);
    }

    @Test
    void shouldRequireApprovedKyc() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.PENDING);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("KYC_REQUIRED", ex.getCode());
        verifyNoInteractions(encryptionPort, accountRepository);
    }

    @Test
    void shouldRefuseRejectedOrDeactivatedBankAccount() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        givenValidPin();
        when(externalBankAccountRepository.findById(externalAccountId))
                .thenReturn(Optional.of(externalAccount(userId, true, BankAccountVerificationStatus.REJECTED)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("BANK_ACCOUNT_NOT_USABLE", ex.getCode());
    }

    @Test
    void shouldRefuseBankNotYetMappedToTheRail() {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        givenValidPin();
        when(externalBankAccountRepository.findById(externalAccountId))
                .thenReturn(Optional.of(externalAccount(userId, true, BankAccountVerificationStatus.VERIFIED)));
        when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank(null)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("BANK_NOT_SYNCED", ex.getCode());
    }

    @Test
    void shouldIgnoreARepeatedRequestThatAlreadyCompleted() {
        when(idempotencyService.tryAcquire("idem-1")).thenReturn(false);
        when(idempotencyService.getResult("idem-1")).thenReturn(Optional.of("tx-previo"));

        payoutUseCase.execute(userId, request("idem-1"), IP);

        verifyNoInteractions(userRepository, accountRepository, payoutGatewayPort);
    }

    @Test
    void shouldRejectARepeatedRequestStillInProgress() {
        when(idempotencyService.tryAcquire("idem-1")).thenReturn(false);
        when(idempotencyService.getResult("idem-1")).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutUseCase.execute(userId, request("idem-1"), IP));

        assertEquals("CONCURRENT_REQUEST", ex.getCode());
        verifyNoInteractions(accountRepository, payoutGatewayPort);
    }

    private void givenValidRequestUpToTheDebit(BigDecimal earnings) {
        givenIdempotencyKeyIsFree();
        givenUser(KycStatus.APPROVED);
        givenValidPin();
        givenOwnedBankAccountAndSyncedBank();
        when(otpService.validateOtp(DOCUMENT, "482913")).thenReturn(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(CoreAccount.builder()
                .id(sourceAccountId)
                .userId(userId)
                .principalBalance(new BigDecimal("2000000"))
                .interestBalance(earnings)
                .status(AccountStatus.ACTIVE)
                .build()));
        org.mockito.Mockito.lenient().when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(externalPayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenIdempotencyKeyIsFree() {
        when(idempotencyService.tryAcquire("idem-1")).thenReturn(true);
    }

    private void givenUser(KycStatus kycStatus) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .documentType(DocumentType.CC)
                .documentNumber(DOCUMENT)
                .firstName("Ana")
                .lastName("Pérez")
                .email("socio@example.com")
                .pinHash("pin-hash")
                .kycStatus(kycStatus)
                .build()));
    }

    private void givenValidPin() {
        when(encryptionPort.decryptRsa("rsa(pin)")).thenReturn("4821");
        when(encryptionPort.verifyPin("4821", "pin-hash")).thenReturn(true);
    }

    private void givenOwnedBankAccountAndSyncedBank() {
        when(externalBankAccountRepository.findById(externalAccountId))
                .thenReturn(Optional.of(externalAccount(userId, true, BankAccountVerificationStatus.VERIFIED)));
        when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1507")));
    }

    private ExternalBankAccount externalAccount(UUID owner, boolean active, BankAccountVerificationStatus status) {
        return ExternalBankAccount.builder()
                .id(externalAccountId)
                .userId(owner)
                .bankCode("1007")
                .accountType(BankAccountType.SAVINGS)
                .accountNumber("00123456789")
                .verificationStatus(status)
                .active(active)
                .build();
    }

    private Bank bank(String wompiBankId) {
        return Bank.builder().code("1007").name("Bancolombia").wompiBankId(wompiBankId).supportsPayout(true).active(true).build();
    }

    private ExecutePayoutRequest request(String idempotencyKey) {
        return new ExecutePayoutRequest(externalAccountId, AMOUNT, "Retiro de ganancias", "rsa(pin)", "482913", idempotencyKey);
    }

    private List<CoreTransaction> savedTransactions() {
        ArgumentCaptor<CoreTransaction> captor = ArgumentCaptor.forClass(CoreTransaction.class);
        verify(transactionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private List<ExternalPayout> savedPayouts() {
        ArgumentCaptor<ExternalPayout> captor = ArgumentCaptor.forClass(ExternalPayout.class);
        verify(externalPayoutRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private PayoutGatewayRequest gatewayRequest() {
        ArgumentCaptor<PayoutGatewayRequest> captor = ArgumentCaptor.forClass(PayoutGatewayRequest.class);
        verify(payoutGatewayPort).initiatePayout(captor.capture());
        return captor.getValue();
    }
}
