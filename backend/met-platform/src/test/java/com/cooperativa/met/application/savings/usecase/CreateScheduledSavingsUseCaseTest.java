package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.application.savings.dto.CreateScheduledSavingsRequest;
import com.cooperativa.met.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.met.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.port.SavingsBalanceCachePort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateScheduledSavingsUseCaseTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private ScheduledSavingsAccountPort accountPort;
    @Mock private SavingsBalanceCachePort balanceCachePort;
    @Mock private ScheduledSavingsMapper mapper;

    @InjectMocks
    private CreateScheduledSavingsUseCase useCase;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void execute_createsAccountSuccessfully_whenUserActiveAndMonthly() {
        // Arrange
        CreateScheduledSavingsRequest request = new CreateScheduledSavingsRequest(
                "Ahorro vacaciones", new BigDecimal("5000000"), new BigDecimal("100000"),
                ContributionFrequency.MONTHLY, null, 15
        );
        ScheduledSavingsAccount savedAccount = ScheduledSavingsAccount.builder()
                .id(UUID.randomUUID()).userId(userId).status(ScheduledSavingsStatus.ACTIVE)
                .currentBalance(BigDecimal.ZERO).build();
        ScheduledSavingsResponse expectedResponse = mock(ScheduledSavingsResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(accountPort.save(any())).thenReturn(savedAccount);
        when(mapper.toResponse(savedAccount)).thenReturn(expectedResponse);

        // Act
        ScheduledSavingsResponse result = useCase.execute(userId, request);

        // Assert
        assertNotNull(result);
        verify(accountPort).save(any(ScheduledSavingsAccount.class));
        verify(balanceCachePort).cacheBalance(eq(savedAccount.getId()), eq(BigDecimal.ZERO));
    }

    @Test
    void execute_createsAccountSuccessfully_whenWeeklyFrequency() {
        // Arrange
        CreateScheduledSavingsRequest request = new CreateScheduledSavingsRequest(
                "Ahorro semanal", new BigDecimal("1000000"), new BigDecimal("50000"),
                ContributionFrequency.WEEKLY, 5, null // Viernes
        );
        ScheduledSavingsAccount savedAccount = ScheduledSavingsAccount.builder()
                .id(UUID.randomUUID()).userId(userId).currentBalance(BigDecimal.ZERO).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(accountPort.save(any())).thenReturn(savedAccount);
        when(mapper.toResponse(any(ScheduledSavingsAccount.class))).thenReturn(mock(ScheduledSavingsResponse.class));

        // Act & Assert — no exception
        assertDoesNotThrow(() -> useCase.execute(userId, request));
    }

    @Test
    void execute_throwsUserNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        CreateScheduledSavingsRequest request = new CreateScheduledSavingsRequest(
                "Test", BigDecimal.TEN, BigDecimal.ONE, ContributionFrequency.MONTHLY, null, 15
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        verifyNoInteractions(accountPort);
    }

    @Test
    void execute_throwsUserNotActive_whenUserIsSuspended() {
        User suspendedUser = User.builder().id(userId).status(UserStatus.SUSPENDED).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));
        CreateScheduledSavingsRequest request = new CreateScheduledSavingsRequest(
                "Test", BigDecimal.TEN, BigDecimal.ONE, ContributionFrequency.MONTHLY, null, 15
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("USER_NOT_ACTIVE", ex.getCode());
        verifyNoInteractions(accountPort);
    }

    @Test
    void execute_throwsInvalidSchedule_whenMonthlyWithoutDayOfMonth() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        CreateScheduledSavingsRequest request = new CreateScheduledSavingsRequest(
                "Test", BigDecimal.TEN, BigDecimal.ONE,
                ContributionFrequency.MONTHLY, null, null // Falta debitDayOfMonth
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("INVALID_SCHEDULE", ex.getCode());
        verifyNoInteractions(accountPort);
    }

    @Test
    void execute_throwsInvalidSchedule_whenWeeklyWithoutDayOfWeek() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        CreateScheduledSavingsRequest request = new CreateScheduledSavingsRequest(
                "Test", BigDecimal.TEN, BigDecimal.ONE,
                ContributionFrequency.WEEKLY, null, null // Falta debitDayOfWeek
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("INVALID_SCHEDULE", ex.getCode());
    }
}
