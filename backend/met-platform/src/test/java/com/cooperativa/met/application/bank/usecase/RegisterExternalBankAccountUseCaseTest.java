package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.bank.dto.BankResponse;
import com.cooperativa.met.application.bank.dto.ExternalBankAccountResponse;
import com.cooperativa.met.application.bank.dto.RegisterExternalBankAccountRequest;
import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterExternalBankAccountUseCaseTest {

    private static final RegisterExternalBankAccountRequest REQUEST =
            new RegisterExternalBankAccountRequest("1007", BankAccountType.SAVINGS, "00123456789");

    @Mock private ExternalBankAccountRepositoryPort externalBankAccountRepository;
    @Mock private BankRepositoryPort bankRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private InitiateBankAccountVerificationUseCase initiateVerificationUseCase;

    @InjectMocks
    private RegisterExternalBankAccountUseCase registerUseCase;
    @InjectMocks
    private ListBanksUseCase listBanksUseCase;
    @InjectMocks
    private ListMyExternalBankAccountsUseCase listMyAccountsUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldRegisterPendingAccountAndSendTheVerificationDeposit() {
        givenUser(KycStatus.APPROVED);
        when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1007", true, true)));
        when(externalBankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExternalBankAccount saved = registerUseCase.execute(userId, REQUEST);

        assertEquals(userId, saved.getUserId());
        assertEquals(BankAccountVerificationStatus.PENDING, saved.getVerificationStatus());
        assertTrue(saved.isActive());
        verify(initiateVerificationUseCase).execute(userId, saved.getId());
        verify(auditLogService).logSuccess(eq(userId), eq("EXTERNAL_BANK_ACCOUNT_REGISTERED"), eq("EXTERNAL_BANK_ACCOUNT"),
                eq(saved.getId().toString()), contains("SAVINGS"));
    }

    @Test
    void shouldKeepTheAccountEvenIfTheVerificationDepositFails() {
        givenUser(KycStatus.APPROVED);
        when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1007", true, true)));
        when(externalBankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new BusinessRuleException("VERIFICATION_SEND_FAILED", "caído")).when(initiateVerificationUseCase).execute(any(), any());

        ExternalBankAccount saved = registerUseCase.execute(userId, REQUEST);

        assertEquals("00123456789", saved.getAccountNumber());
    }

    @Test
    void shouldRequireApprovedKyc() {
        givenUser(KycStatus.PENDING);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> registerUseCase.execute(userId, REQUEST));

        assertEquals("KYC_REQUIRED", ex.getCode());
        verifyNoInteractions(externalBankAccountRepository, initiateVerificationUseCase);
    }

    @Test
    void shouldRefuseBankThatDoesNotSupportWithdrawals() {
        givenUser(KycStatus.APPROVED);
        when(bankRepository.findByCode("1007")).thenReturn(Optional.of(bank("1007", true, false)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> registerUseCase.execute(userId, REQUEST));

        assertEquals("BANK_NOT_SUPPORTED", ex.getCode());
        verify(externalBankAccountRepository, never()).save(any());
    }

    @Test
    void shouldRefuseUnknownBank() {
        givenUser(KycStatus.APPROVED);
        when(bankRepository.findByCode("1007")).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> registerUseCase.execute(userId, REQUEST));

        assertEquals("BANK_NOT_FOUND", ex.getCode());
    }

    @Test
    void shouldListOnlyBanksOfTheRequestedCatalog() {
        Bank pseOnly = bank("1051", true, false).toBuilder().supportsPse(true).build();
        Bank payoutOnly = bank("1007", true, true);
        when(bankRepository.findAllActive()).thenReturn(List.of(pseOnly, payoutOnly));

        List<BankResponse> payout = listBanksUseCase.execute(ListBanksUseCase.CatalogType.PAYOUT);
        List<BankResponse> pse = listBanksUseCase.execute(ListBanksUseCase.CatalogType.PSE);

        assertEquals(List.of("1007"), payout.stream().map(BankResponse::code).toList());
        assertEquals(List.of("1051"), pse.stream().map(BankResponse::code).toList());
    }

    @Test
    void shouldShowOnlyTheLastFourDigitsOfEachAccount() {
        when(externalBankAccountRepository.findActiveByUserId(userId)).thenReturn(List.of(
                account("00123456789", "1007", 2731),
                account("123", "9999", null)));
        when(bankRepository.findAllActive()).thenReturn(List.of(bank("1007", true, true)));

        List<ExternalBankAccountResponse> accounts = listMyAccountsUseCase.execute(userId);

        assertEquals("****6789", accounts.get(0).maskedAccountNumber());
        assertEquals("Banco 1007", accounts.get(0).bankName());
        assertTrue(accounts.get(0).verificationPending());
        assertEquals("****", accounts.get(1).maskedAccountNumber(), "un número corto no se muestra");
        assertEquals("9999", accounts.get(1).bankName(), "banco sin catálogo muestra el código");
        assertFalse(accounts.get(1).verificationPending());
    }

    private void givenUser(KycStatus kycStatus) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).kycStatus(kycStatus).build()));
    }

    private Bank bank(String code, boolean active, boolean supportsPayout) {
        return Bank.builder().code(code).name("Banco " + code).active(active).supportsPayout(supportsPayout).build();
    }

    private ExternalBankAccount account(String number, String bankCode, Integer pendingAmount) {
        return ExternalBankAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .bankCode(bankCode)
                .accountType(BankAccountType.SAVINGS)
                .accountNumber(number)
                .verificationStatus(BankAccountVerificationStatus.PENDING)
                .pendingVerificationAmount(pendingAmount)
                .active(true)
                .build();
    }
}
