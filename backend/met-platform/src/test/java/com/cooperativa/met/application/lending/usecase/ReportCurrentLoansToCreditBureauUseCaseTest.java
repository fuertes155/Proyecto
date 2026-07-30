package com.cooperativa.met.application.lending.usecase;

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
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
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
import static org.mockito.Mockito.*;

class ReportCurrentLoansToCreditBureauUseCaseTest {

    @Mock
    private PersonalLoanApplicationPort loanPort;
    @Mock
    private AmortizationSchedulePort schedulePort;
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private CreditBureauPort creditBureauPort;

    @InjectMocks
    private ReportCurrentLoansToCreditBureauUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void reportsAlDia_forLoanWithNoLateInstallments() {
        UUID loanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId)
                .userId(userId)
                .amount(new BigDecimal("1000000"))
                .status(LoanApplicationStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        AmortizationInstallment paid = AmortizationInstallment.builder()
                .id(UUID.randomUUID()).applicationId(loanId).installmentNumber(1)
                .remainingBalance(new BigDecimal("700000")).status("PAID").build();
        AmortizationInstallment pending = AmortizationInstallment.builder()
                .id(UUID.randomUUID()).applicationId(loanId).installmentNumber(2)
                .remainingBalance(new BigDecimal("400000")).status("PENDING").build();

        User user = User.builder()
                .id(userId).documentType(DocumentType.CC).documentNumber("123456")
                .firstName("Test").lastName("User")
                .status(UserStatus.ACTIVE).kycStatus(KycStatus.APPROVED)
                .build();

        when(loanPort.findByStatus(LoanApplicationStatus.APPROVED)).thenReturn(List.of(loan));
        when(schedulePort.findByApplicationId(loanId)).thenReturn(List.of(paid, pending));
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute();

        ArgumentCaptor<CreditReportEvent> captor = ArgumentCaptor.forClass(CreditReportEvent.class);
        verify(creditBureauPort, times(1)).reportCreditBehavior(captor.capture());
        assertEquals(CreditReportEventType.AL_DIA, captor.getValue().getEventType());
        assertEquals(loanId, captor.getValue().getLoanId());
        // Debe usar el saldo restante de la última cuota PAGADA, no la pendiente
        assertEquals(new BigDecimal("700000"), captor.getValue().getOutstandingBalance());
    }

    @Test
    void skipsLoan_whenItHasALateInstallment() {
        UUID loanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId)
                .userId(userId)
                .amount(new BigDecimal("1000000"))
                .status(LoanApplicationStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        AmortizationInstallment late = AmortizationInstallment.builder()
                .id(UUID.randomUUID()).applicationId(loanId).installmentNumber(1)
                .dueDate(LocalDate.now().minusDays(10))
                .remainingBalance(new BigDecimal("900000")).status("LATE").build();

        when(loanPort.findByStatus(LoanApplicationStatus.APPROVED)).thenReturn(List.of(loan));
        when(schedulePort.findByApplicationId(loanId)).thenReturn(List.of(late));

        useCase.execute();

        verify(creditBureauPort, never()).reportCreditBehavior(any());
        verifyNoInteractions(userRepositoryPort);
    }
}
