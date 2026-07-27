package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.lending.port.MessagingPort;
import com.cooperativa.met.domain.lending.port.PaymentGatewayPort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

        AmortizationInstallment installment = new AmortizationInstallment();
        installment.setId(UUID.randomUUID());
        installment.setApplicationId(loanId);
        installment.setDueDate(today);
        installment.setPaymentAmount(new BigDecimal("100000"));
        installment.setPrincipalAmount(new BigDecimal("80000"));

        PersonalLoanApplication loan = new PersonalLoanApplication();
        loan.setId(loanId);
        loan.setUserId(userId);

        User user = new User();
        user.setId(userId);
        user.setPaymentCardToken("tok_12345");

        when(schedulePort.findPendingInstallmentsByDueDateBeforeOrEqual(any(LocalDate.class))).thenReturn(List.of(installment));
        when(loanPort.findById(loanId)).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
        when(paymentGatewayPort.chargeTokenizedCard(eq(userId), eq("tok_12345"), any(BigDecimal.class), anyString())).thenReturn(true);

        processLoanCollectionsUseCase.execute();

        verify(paymentGatewayPort, times(1)).chargeTokenizedCard(any(), any(), any(), any());
        verify(schedulePort, times(1)).saveAll(eq(loanId), anyList());
        verify(messagingPort, never()).sendSms(anyString(), anyString());
    }
}
