package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.investment.usecase.GuaranteeFundCompensationUseCase;
import com.cooperativa.met.application.investment.usecase.PaymentDistributionUseCase;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.CreditReportEvent;
import com.cooperativa.met.domain.lending.model.CreditReportEventType;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import com.cooperativa.met.domain.lending.port.MessagingPort;
import com.cooperativa.met.domain.lending.port.PaymentGatewayPort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProcessLoanCollectionsUseCaseTest {

    @Mock
    private AmortizationSchedulePort schedulePort;
    @Mock
    private PersonalLoanApplicationPort loanPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private MessagingPort messagingPort;
    @Mock
    private CreditBureauPort creditBureauPort;
    @Mock
    private PaymentDistributionUseCase paymentDistributionUseCase;
    @Mock
    private GuaranteeFundCompensationUseCase guaranteeFundCompensationUseCase;
    @Mock
    private CapitalEngineProperties capitalEngineProperties;

    @InjectMocks
    private ProcessLoanCollectionsUseCase processLoanCollectionsUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProcessDueInstallmentsSuccessfullyWithToken() {
        LocalDate today = LocalDate.now();
        UUID loanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AmortizationInstallment installment = AmortizationInstallment.builder()
                .id(UUID.randomUUID())
                .applicationId(loanId)
                .installmentNumber(1)
                .paymentAmount(new BigDecimal("100000"))
                .principalAmount(new BigDecimal("80000"))
                .interestAmount(new BigDecimal("20000"))
                .remainingBalance(new BigDecimal("500000"))
                .dueDate(today)
                .penaltyInterestAmount(BigDecimal.ZERO)
                .status("PENDING")
                .build();

        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId)
                .userId(userId)
                .amount(new BigDecimal("1000000"))
                .termMonths(12)
                .annualInterestRate(new BigDecimal("0.18"))
                .monthlyPayment(new BigDecimal("100000"))
                .totalInterest(new BigDecimal("200000"))
                .totalPayment(new BigDecimal("1200000"))
                .purpose("Personal")
                .status(LoanApplicationStatus.DISBURSED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User user = User.builder()
                .id(userId)
                .documentType(DocumentType.CC)
                .documentNumber("123456")
                .email("test@test.com")
                .phone("3001234567")
                .firstName("Test")
                .lastName("User")
                .pinHash("hash")
                .biometricHash("")
                .failedLoginAttempts(0)
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .termsAccepted(true)
                .termsAcceptedAt(Instant.now())
                .emailNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastKnownIp("")
                .paymentCardToken("tok_12345")
                .lastKnownDeviceId("")
                .build();

        when(schedulePort.findPendingInstallmentsByDueDateBeforeOrEqual(any(LocalDate.class))).thenReturn(List.of(installment));
        when(loanPort.findById(loanId)).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
        when(paymentGatewayPort.chargeTokenizedCard(eq(userId), eq("tok_12345"), any(BigDecimal.class), anyString())).thenReturn(true);

        processLoanCollectionsUseCase.execute();

        verify(paymentGatewayPort, times(1)).chargeTokenizedCard(any(), any(), any(), any());
        verify(schedulePort, times(1)).saveAll(eq(loanId), anyList());
        verify(messagingPort, never()).sendSms(anyString(), anyString());
        verify(creditBureauPort, never()).reportCreditBehavior(any());
    }

    @Test
    void shouldReportPagado_whenLastInstallmentIsPaidOff() {
        LocalDate today = LocalDate.now();
        UUID loanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AmortizationInstallment lastInstallment = AmortizationInstallment.builder()
                .id(UUID.randomUUID())
                .applicationId(loanId)
                .installmentNumber(1)
                .paymentAmount(new BigDecimal("100000"))
                .principalAmount(new BigDecimal("100000"))
                .interestAmount(BigDecimal.ZERO)
                .remainingBalance(BigDecimal.ZERO)
                .dueDate(today)
                .penaltyInterestAmount(BigDecimal.ZERO)
                .status("PENDING")
                .build();

        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId)
                .userId(userId)
                .amount(new BigDecimal("100000"))
                .termMonths(1)
                .annualInterestRate(new BigDecimal("0.18"))
                .status(LoanApplicationStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User user = User.builder()
                .id(userId)
                .documentType(DocumentType.CC)
                .documentNumber("123456")
                .firstName("Test")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .paymentCardToken("tok_12345")
                .build();

        when(schedulePort.findPendingInstallmentsByDueDateBeforeOrEqual(any(LocalDate.class))).thenReturn(List.of(lastInstallment));
        when(loanPort.findById(loanId)).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
        when(paymentGatewayPort.chargeTokenizedCard(eq(userId), eq("tok_12345"), any(BigDecimal.class), anyString())).thenReturn(true);
        when(schedulePort.findByApplicationId(loanId)).thenReturn(List.of(lastInstallment.withStatus("PAID")));

        processLoanCollectionsUseCase.execute();

        ArgumentCaptor<CreditReportEvent> eventCaptor = ArgumentCaptor.forClass(CreditReportEvent.class);
        verify(creditBureauPort, times(1)).reportCreditBehavior(eventCaptor.capture());
        assertEquals(CreditReportEventType.PAGADO, eventCaptor.getValue().getEventType());
        assertEquals(loanId, eventCaptor.getValue().getLoanId());

        ArgumentCaptor<PersonalLoanApplication> loanCaptor = ArgumentCaptor.forClass(PersonalLoanApplication.class);
        verify(loanPort, times(1)).save(loanCaptor.capture());
        assertEquals(LoanApplicationStatus.PAID_OFF, loanCaptor.getValue().getStatus());
    }

    @Test
    void shouldReportMora_whenInstallmentIsSixDaysLate() {
        LocalDate today = LocalDate.now();
        UUID loanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AmortizationInstallment installment = AmortizationInstallment.builder()
                .id(UUID.randomUUID())
                .applicationId(loanId)
                .installmentNumber(1)
                .paymentAmount(new BigDecimal("100000"))
                .principalAmount(new BigDecimal("80000"))
                .interestAmount(new BigDecimal("20000"))
                .remainingBalance(new BigDecimal("500000"))
                .dueDate(today.minusDays(6))
                .penaltyInterestAmount(BigDecimal.ZERO)
                .status("PENDING")
                .build();

        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId)
                .userId(userId)
                .amount(new BigDecimal("1000000"))
                .termMonths(12)
                .status(LoanApplicationStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User user = User.builder()
                .id(userId)
                .documentType(DocumentType.CC)
                .documentNumber("123456")
                .phone("3001234567")
                .firstName("Test")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .paymentCardToken(null)
                .build();

        when(schedulePort.findPendingInstallmentsByDueDateBeforeOrEqual(any(LocalDate.class))).thenReturn(List.of(installment));
        when(loanPort.findById(loanId)).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

        processLoanCollectionsUseCase.execute();

        ArgumentCaptor<CreditReportEvent> eventCaptor = ArgumentCaptor.forClass(CreditReportEvent.class);
        verify(creditBureauPort, times(1)).reportCreditBehavior(eventCaptor.capture());
        assertEquals(CreditReportEventType.MORA, eventCaptor.getValue().getEventType());
        assertEquals(6, eventCaptor.getValue().getDaysLate());
        verify(loanPort, never()).save(any());
    }
}
