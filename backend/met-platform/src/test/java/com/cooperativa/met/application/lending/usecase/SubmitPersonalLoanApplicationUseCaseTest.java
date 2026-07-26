package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.met.application.lending.dto.SubmitLoanApplicationRequest;
import com.cooperativa.met.application.lending.mapper.LendingMapper;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.CreditScoreResult;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitPersonalLoanApplicationUseCaseTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private PersonalLoanApplicationPort applicationPort;
    @Mock private AmortizationSchedulePort schedulePort;
    @Mock private ComplianceCheckPort complianceCheckPort;
    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private CoreTransactionRepositoryPort transactionRepository;
    @Mock private CreditBureauPort creditBureauPort;
    @Mock private LendingMapper mapper;

    @InjectMocks
    private SubmitPersonalLoanApplicationUseCase useCase;

    private UUID userId;
    private User activeUser;
    private CoreAccount userAccount;

    @BeforeEach
    void setUp() {
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
                .principalBalance(new BigDecimal("5000000.00")) // Permite préstamo hasta 50M
                .build();

        // Inyectar el @Value field
        ReflectionTestUtils.setField(useCase, "minCreditScore", 600);
    }

    @Test
    void execute_submitLoanSuccessfully_withGoodCreditScore() {
        // Arrange
        SubmitLoanApplicationRequest request = buildRequest(new BigDecimal("10000000"), 24, true);
        CreditScoreResult goodScore = CreditScoreResult.builder()
                .score(750).referenceId("REF-001").build();
        PersonalLoanApplication savedApp = mock(PersonalLoanApplication.class);
        LoanApplicationResponse expectedResponse = mock(LoanApplicationResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(applicationPort.hasPendingApplication(userId)).thenReturn(false);
        when(complianceCheckPort.checkUser(any(), any())).thenReturn(ComplianceResult.CLEAR);
        when(creditBureauPort.checkScore(any(), any(), any(), any(), any())).thenReturn(goodScore);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(userAccount));
        when(applicationPort.save(any())).thenReturn(savedApp);
        when(schedulePort.saveAll(any(), any())).thenReturn(List.of());
        when(mapper.toResponse(savedApp, List.of())).thenReturn(expectedResponse);

        // Act
        LoanApplicationResponse result = useCase.execute(userId, request);

        // Assert
        assertNotNull(result);
        verify(applicationPort).save(any(PersonalLoanApplication.class));
    }

    @Test
    void execute_throwsUserNotActive_whenUserIsSuspended() {
        User suspended = User.builder().id(userId).status(UserStatus.SUSPENDED).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspended));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, buildRequest(BigDecimal.TEN, 12, true)));

        assertEquals("USER_NOT_ACTIVE", ex.getCode());
        verifyNoInteractions(applicationPort);
    }

    @Test
    void execute_throwsPendingApplicationExists_whenAlreadyHasPendingLoan() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(applicationPort.hasPendingApplication(userId)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, buildRequest(BigDecimal.TEN, 12, true)));

        assertEquals("PENDING_APPLICATION_EXISTS", ex.getCode());
    }

    @Test
    void execute_throwsComplianceMatch_whenUserIsOnRestrictiveList() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(applicationPort.hasPendingApplication(userId)).thenReturn(false);
        // Primer check de compliance devuelve MATCH
        when(complianceCheckPort.checkUser(any(), eq(ComplianceListType.values()[0])))
                .thenReturn(ComplianceResult.MATCH);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, buildRequest(BigDecimal.TEN, 12, true)));

        assertEquals("COMPLIANCE_MATCH", ex.getCode());
        verify(applicationPort, never()).save(any());
    }

    @Test
    void execute_throwsHabeasDataRequired_whenNotAccepted() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(applicationPort.hasPendingApplication(userId)).thenReturn(false);
        when(complianceCheckPort.checkUser(any(), any())).thenReturn(ComplianceResult.CLEAR);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, buildRequest(BigDecimal.TEN, 12, false)));

        assertEquals("HABEAS_DATA_REQUIRED", ex.getCode());
    }

    @Test
    void execute_throwsRiskScoreLow_whenCreditScoreBelowMinimum() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(applicationPort.hasPendingApplication(userId)).thenReturn(false);
        when(complianceCheckPort.checkUser(any(), any())).thenReturn(ComplianceResult.CLEAR);
        CreditScoreResult badScore = CreditScoreResult.builder().score(450).referenceId("REF-002").build();
        when(creditBureauPort.checkScore(any(), any(), any(), any(), any())).thenReturn(badScore);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, buildRequest(BigDecimal.TEN, 12, true)));

        assertEquals("RISK_SCORE_LOW", ex.getCode());
        verify(applicationPort, never()).save(any());
    }

    @Test
    void execute_throwsMaxLoanExceeded_whenAmountExceedsTenTimesBalance() {
        // Balance: 5,000,000 → max loan: 50,000,000. Solicitamos 60,000,000
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(applicationPort.hasPendingApplication(userId)).thenReturn(false);
        when(complianceCheckPort.checkUser(any(), any())).thenReturn(ComplianceResult.CLEAR);
        CreditScoreResult goodScore = CreditScoreResult.builder().score(750).referenceId("REF-003").build();
        when(creditBureauPort.checkScore(any(), any(), any(), any(), any())).thenReturn(goodScore);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(userAccount));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, buildRequest(new BigDecimal("60000000"), 24, true)));

        assertEquals("MAX_LOAN_EXCEEDED", ex.getCode());
        verify(applicationPort, never()).save(any());
    }

    private SubmitLoanApplicationRequest buildRequest(BigDecimal amount, int termMonths, boolean habeasData) {
        return new SubmitLoanApplicationRequest(amount, termMonths, "Personal", habeasData);
    }
}
