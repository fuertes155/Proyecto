package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.domain.notification.model.Notification;
import com.cooperativa.met.domain.notification.port.NotificationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLoanUseCaseTest {

    @Mock
    private PersonalLoanApplicationPort loanApplicationPort;
    @Mock
    private CoreAccountRepositoryPort accountRepository;
    @Mock
    private CoreTransactionRepositoryPort transactionRepository;
    @Mock
    private NotificationRepositoryPort notificationRepository;
    @Mock
    private FeeScheduleRepositoryPort feeRepository;

    @InjectMocks
    private AdminLoanUseCase adminLoanUseCase;

    private UUID loanId;
    private UUID userId;
    private PersonalLoanApplication loanApp;
    private CoreAccount account;
    private FeeSchedule fee;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        loanApp = PersonalLoanApplication.builder()
                .id(loanId)
                .userId(userId)
                .amount(new BigDecimal("1000000.00"))
                .creditScore(750) // RIESGO_MEDIO: hasta 2x saldo de ahorro
                .status(LoanApplicationStatus.IN_REVIEW)
                .build();

        account = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(AccountStatus.ACTIVE)
                .principalBalance(new BigDecimal("1000000.00")) // 2x = 2,000,000 >= monto del préstamo
                .build();
                
        fee = FeeSchedule.builder()
                .tipoTarifa("FONDO_GARANTIAS")
                .valor(new BigDecimal("20000.00"))
                .build();
    }

    @Test
    void updateLoanStatus_approved_disbursesAndNotifies() {
        // Arrange
        when(loanApplicationPort.findById(loanId)).thenReturn(Optional.of(loanApp));
        when(feeRepository.findVigentes()).thenReturn(List.of(fee));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        
        PersonalLoanApplication updatedLoanApp = loanApp.toBuilder().status(LoanApplicationStatus.APPROVED).build();
        when(loanApplicationPort.save(any(PersonalLoanApplication.class))).thenReturn(updatedLoanApp);

        // Act
        PersonalLoanApplication result = adminLoanUseCase.updateLoanStatus(loanId, LoanApplicationStatus.APPROVED);

        // Assert
        assertEquals(LoanApplicationStatus.APPROVED, result.getStatus());
        
        ArgumentCaptor<CoreAccount> accountCaptor = ArgumentCaptor.forClass(CoreAccount.class);
        verify(accountRepository).save(accountCaptor.capture());
        
        CoreAccount savedAccount = accountCaptor.getValue();
        // 1,000,000 + (1,000,000 - 20,000) = 1,980,000
        assertEquals(new BigDecimal("1980000.00"), savedAccount.getPrincipalBalance());
        
        verify(transactionRepository).save(any(CoreTransaction.class));
        
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertEquals("Préstamo Aprobado 🎉", notifCaptor.getValue().getTitle());
    }

    @Test
    void updateLoanStatus_approved_throwsMaxLoanExceeded_whenBalanceNoLongerSupportsRiskTier() {
        // El saldo bajó desde la solicitud: score 750 (RIESGO_MEDIO, 2x) sobre 100,000 -> máximo 200,000 < 1,000,000
        CoreAccount lowBalanceAccount = account.toBuilder().principalBalance(new BigDecimal("100000.00")).build();
        when(loanApplicationPort.findById(loanId)).thenReturn(Optional.of(loanApp));
        when(feeRepository.findVigentes()).thenReturn(List.of(fee));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(lowBalanceAccount));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adminLoanUseCase.updateLoanStatus(loanId, LoanApplicationStatus.APPROVED));

        assertEquals("MAX_LOAN_EXCEEDED", ex.getCode());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(loanApplicationPort, never()).save(any());
    }

    @Test
    void updateLoanStatus_rejected_notifiesUserAndDoesNotDisburse() {
        // Arrange
        when(loanApplicationPort.findById(loanId)).thenReturn(Optional.of(loanApp));
        
        PersonalLoanApplication updatedLoanApp = loanApp.toBuilder().status(LoanApplicationStatus.REJECTED).build();
        when(loanApplicationPort.save(any(PersonalLoanApplication.class))).thenReturn(updatedLoanApp);

        // Act
        PersonalLoanApplication result = adminLoanUseCase.updateLoanStatus(loanId, LoanApplicationStatus.REJECTED);

        // Assert
        assertEquals(LoanApplicationStatus.REJECTED, result.getStatus());
        
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(feeRepository);
        
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertEquals("Préstamo Rechazado ❌", notifCaptor.getValue().getTitle());
    }
}
