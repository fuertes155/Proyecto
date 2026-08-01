package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.lending.dto.LoanEligibilityRequest;
import com.cooperativa.met.application.lending.dto.LoanEligibilityResponse;
import com.cooperativa.met.application.lending.mapper.LendingMapper;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.CreditScoreResult;
import com.cooperativa.met.domain.lending.model.RiskTier;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetLoanEligibilityUseCaseTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private CreditBureauPort creditBureauPort;

    private final LendingMapper mapper = new LendingMapper();

    private GetLoanEligibilityUseCase useCase;

    private UUID userId;
    private User activeUser;
    private CoreAccount userAccount;

    @BeforeEach
    void setUp() {
        useCase = new GetLoanEligibilityUseCase(userRepository, accountRepository, creditBureauPort, mapper);

        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .documentNumber("12345678")
                .firstName("Juan")
                .lastName("Pérez")
                .build();
        userAccount = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(AccountStatus.ACTIVE)
                .principalBalance(new BigDecimal("5000000.00"))
                .build();
    }

    @Test
    void execute_returnsPersonalizedLimits_forGoodCreditScore() {
        // score 750 -> RIESGO_MEDIO: 2x saldo (5,000,000) = 10,000,000, tope 24 meses, 22%
        CreditScoreResult goodScore = CreditScoreResult.builder().score(750).referenceId("REF-001").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(creditBureauPort.checkScore(any(), any(), any(), any(), any())).thenReturn(goodScore);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(userAccount));

        LoanEligibilityResponse response = useCase.execute(userId, new LoanEligibilityRequest(true));

        assertTrue(response.approved());
        assertEquals("RIESGO_MEDIO", response.tier());
        assertEquals(new BigDecimal("10000000.00"), response.maxAmount());
        assertEquals(24, response.maxTermMonths());
        assertEquals(new BigDecimal("0.22"), response.annualInterestRate());
    }

    @Test
    void execute_returnsRejected_whenCreditScoreBelowMinimum_withoutRequiringAccount() {
        CreditScoreResult badScore = CreditScoreResult.builder().score(450).referenceId("REF-002").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(creditBureauPort.checkScore(any(), any(), any(), any(), any())).thenReturn(badScore);

        LoanEligibilityResponse response = useCase.execute(userId, new LoanEligibilityRequest(true));

        assertFalse(response.approved());
        assertEquals("RECHAZADO", response.tier());
        assertEquals(BigDecimal.ZERO, response.maxAmount());
        verifyNoInteractions(accountRepository);
    }

    @Test
    void execute_throwsUserNotActive_whenUserIsSuspended() {
        User suspended = User.builder().id(userId).status(UserStatus.SUSPENDED).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspended));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, new LoanEligibilityRequest(true)));

        assertEquals("USER_NOT_ACTIVE", ex.getCode());
        verifyNoInteractions(creditBureauPort);
    }

    @Test
    void execute_throwsHabeasDataRequired_whenNotAccepted() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, new LoanEligibilityRequest(false)));

        assertEquals("HABEAS_DATA_REQUIRED", ex.getCode());
        verifyNoInteractions(creditBureauPort);
    }

    @Test
    void execute_throwsNoAccount_whenApprovedButUserHasNoWallet() {
        CreditScoreResult goodScore = CreditScoreResult.builder().score(750).referenceId("REF-003").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(creditBureauPort.checkScore(any(), any(), any(), any(), any())).thenReturn(goodScore);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, new LoanEligibilityRequest(true)));

        assertEquals("NO_ACCOUNT", ex.getCode());
    }

    @Test
    void execute_returnsPrimeAbsoluteCap_whenSavingsMultiplierWouldExceedIt() {
        // score 920 -> PRIME: 5x saldo (20,000,000) = 100,000,000, pero tope absoluto 60,000,000
        CreditScoreResult primeScore = CreditScoreResult.builder().score(920).referenceId("REF-004").build();
        CoreAccount richAccount = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(AccountStatus.ACTIVE)
                .principalBalance(new BigDecimal("20000000.00"))
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(creditBureauPort.checkScore(any(), any(), any(), any(), any())).thenReturn(primeScore);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(richAccount));

        LoanEligibilityResponse response = useCase.execute(userId, new LoanEligibilityRequest(true));

        assertTrue(response.approved());
        assertEquals("PRIME", response.tier());
        assertEquals(new BigDecimal("60000000"), response.maxAmount());
        assertEquals(48, response.maxTermMonths());
    }
}
