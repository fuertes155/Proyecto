package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.CreateNativePseDepositRequest;
import com.cooperativa.met.application.account.dto.CreateNativePseDepositResponse;
import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.account.service.AccountReversalService;
import com.cooperativa.met.application.account.service.PaymentReturnUrlResolver;
import com.cooperativa.met.application.account.service.TransferLimitService;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import com.cooperativa.met.domain.bank.port.PseTransactionRequest;
import com.cooperativa.met.domain.bank.port.PseTransactionResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.webhook.entity.ProcessedWebhookJpaEntity;
import com.cooperativa.met.infrastructure.persistence.webhook.repository.ProcessedWebhookJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Límites de transferencia, reversiones, recargas PSE y webhooks de depósito. */
@ExtendWith(MockitoExtension.class)
class AccountMoneyMovementsTest {

    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private CoreTransactionRepositoryPort transactionRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private BankRepositoryPort bankRepository;
    @Mock private PseGatewayPort pseGatewayPort;
    @Mock private PaymentReturnUrlResolver returnUrlResolver;
    @Mock private ProcessedWebhookJpaRepository processedWebhookRepository;
    @Mock private DepositUseCase depositUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @Test
    void shouldRejectTransferAbovePerTransactionLimit() {
        TransferLimitService limits = new TransferLimitService(transactionRepository);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> limits.validateTransferLimits(account(), new BigDecimal("2000000.01")));

        assertEquals("PER_TX_LIMIT_EXCEEDED", ex.getCode());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldRejectTransferThatWouldExceedTheDailyLimit() {
        TransferLimitService limits = new TransferLimitService(transactionRepository);
        when(transactionRepository.sumOutgoingTransfersByAccountIdAndDateRange(eq(accountId), any(), any()))
                .thenReturn(new BigDecimal("4900000.00"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> limits.validateTransferLimits(account(), new BigDecimal("150000")));

        assertEquals("DAILY_LIMIT_EXCEEDED", ex.getCode());
    }

    @Test
    void shouldAllowSmallTransfersAtAnyHour() {
        TransferLimitService limits = new TransferLimitService(transactionRepository);
        when(transactionRepository.sumOutgoingTransfersByAccountIdAndDateRange(eq(accountId), any(), any())).thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() -> limits.validateTransferLimits(account(), new BigDecimal("200000")));
    }

    @Test
    void shouldBlockLargeTransfersAtNightWithTheTimeVault() {
        // La "bóveda de tiempo" depende de la hora real de Bogotá: este caso solo se puede verificar de 00:00 a 06:00.
        assumeTrue(ZonedDateTime.now(ZoneId.of("America/Bogota")).getHour() < 6, "solo aplica en la madrugada");
        TransferLimitService limits = new TransferLimitService(transactionRepository);
        when(transactionRepository.sumOutgoingTransfersByAccountIdAndDateRange(eq(accountId), any(), any())).thenReturn(BigDecimal.ZERO);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> limits.validateTransferLimits(account(), new BigDecimal("200001")));

        assertEquals("TIME_VAULT_ACTIVE", ex.getCode());
    }

    @Test
    void shouldRetryReversalOnConcurrentUpdateUntilItSucceeds() {
        AccountReversalService reversal = new AccountReversalService(accountRepository);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(accountRepository.save(any()))
                .thenThrow(new OptimisticLockingFailureException("otra escritura"))
                .thenAnswer(inv -> inv.getArgument(0));

        CoreAccount credited = reversal.creditWithRetry(accountId, new BigDecimal("150000"));

        assertEquals(new BigDecimal("185000"), credited.getInterestBalance());
        verify(accountRepository, times(2)).save(any());
    }

    @Test
    void shouldGiveUpAfterThreeConflictsSoTheReversalIsReconciledByHand() {
        AccountReversalService reversal = new AccountReversalService(accountRepository);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account()));
        when(accountRepository.save(any())).thenThrow(new OptimisticLockingFailureException("conflicto"));

        assertThrows(OptimisticLockingFailureException.class, () -> reversal.creditWithRetry(accountId, new BigDecimal("150000")));

        verify(accountRepository, times(3)).save(any());
    }

    @Test
    void shouldStartPseTopUpWithTheUsersOwnData() {
        CreateNativePseDepositUseCase pse = pseUseCase();
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).firstName(" Ana ").lastName("Pérez")
                .email("socio@example.com").phone("3001234567").documentType(DocumentType.CC).documentNumber("1086578123").build()));
        when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1007", true, "1040")));
        when(returnUrlResolver.resolve(userId, "metapp://deposito")).thenReturn("https://met.app/retorno");
        when(pseGatewayPort.createPseTransaction(any())).thenReturn(new PseTransactionResult("wompi-1", "https://banco/pse", "PENDING"));

        CreateNativePseDepositResponse response = pse.execute(userId, pseRequest("1007"));

        assertEquals("https://banco/pse", response.getAsyncPaymentUrl());
        ArgumentCaptor<PseTransactionRequest> sent = ArgumentCaptor.forClass(PseTransactionRequest.class);
        verify(pseGatewayPort).createPseTransaction(sent.capture());
        assertEquals("Ana Pérez", sent.getValue().customerFullName());
        assertEquals("1040", sent.getValue().financialInstitutionCode());
        assertEquals("https://met.app/retorno", sent.getValue().redirectUrl());
        assertEquals(new BigDecimal("50000"), sent.getValue().amount());
    }

    @Test
    void shouldRefusePseWithBanksThatAreNotReady() {
        CreateNativePseDepositUseCase pse = pseUseCase();
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(bankRepository.findByCode("0001")).thenReturn(Optional.of(bank("0001", false, "1")));
        when(bankRepository.findByCode("0002")).thenReturn(Optional.of(bank("0002", true, "")));
        when(bankRepository.findByCode("0003")).thenReturn(Optional.empty());

        assertEquals("BANK_NOT_SUPPORTED", assertThrows(BusinessRuleException.class, () -> pse.execute(userId, pseRequest("0001"))).getCode());
        assertEquals("BANK_NOT_SYNCED", assertThrows(BusinessRuleException.class, () -> pse.execute(userId, pseRequest("0002"))).getCode());
        assertEquals("BANK_NOT_FOUND", assertThrows(BusinessRuleException.class, () -> pse.execute(userId, pseRequest("0003"))).getCode());
        verifyNoInteractions(pseGatewayPort);
    }

    @Test
    void shouldRefusePseForInactiveAccount() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account().toBuilder().status(AccountStatus.BLOCKED).build()));

        assertEquals("DEP_ERR_03", assertThrows(BusinessRuleException.class,
                () -> pseUseCase().execute(userId, pseRequest("1007"))).getCode());
    }

    @Test
    void shouldCreditDepositFromWebhookOnlyOnce() {
        ProcessWebhookUseCase webhook = new ProcessWebhookUseCase(processedWebhookRepository, depositUseCase);
        when(processedWebhookRepository.existsById("wompi-1")).thenReturn(false, true);

        webhook.execute("wompi-1", "WOMPI", userId, new BigDecimal("50000"), "PSE");
        webhook.execute("wompi-1", "WOMPI", userId, new BigDecimal("50000"), "PSE");

        ArgumentCaptor<DepositRequest> deposit = ArgumentCaptor.forClass(DepositRequest.class);
        verify(depositUseCase, times(1)).execute(eq(userId), deposit.capture());
        assertEquals(new BigDecimal("50000"), deposit.getValue().getAmount());
        verify(processedWebhookRepository).saveAndFlush(any(ProcessedWebhookJpaEntity.class));
    }

    @Test
    void shouldNotCreditWhenAnotherThreadAlreadyClaimedTheWebhook() {
        ProcessWebhookUseCase webhook = new ProcessWebhookUseCase(processedWebhookRepository, depositUseCase);
        when(processedWebhookRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        webhook.execute("wompi-1", "WOMPI", userId, new BigDecimal("50000"), "PSE");

        verifyNoInteractions(depositUseCase);
    }

    private CreateNativePseDepositUseCase pseUseCase() {
        return new CreateNativePseDepositUseCase(accountRepository, userRepository, bankRepository, pseGatewayPort, returnUrlResolver);
    }

    private CreateNativePseDepositRequest pseRequest(String bankCode) {
        CreateNativePseDepositRequest request = new CreateNativePseDepositRequest();
        request.setAmount(new BigDecimal("50000"));
        request.setBankCode(bankCode);
        request.setReturnUrl("metapp://deposito");
        return request;
    }

    private Bank bank(String code, boolean supportsPse, String pseCode) {
        return Bank.builder().code(code).name("Banco " + code).active(true).supportsPse(supportsPse).wompiPseCode(pseCode).build();
    }

    private CoreAccount account() {
        return CoreAccount.builder()
                .id(accountId)
                .userId(userId)
                .accountNumber("1000012345")
                .principalBalance(new BigDecimal("1200000"))
                .interestBalance(new BigDecimal("35000"))
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
