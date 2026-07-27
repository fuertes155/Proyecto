package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.investment.usecase.DistributeDepositUseCase;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DepositUseCaseTest {

    @Mock
    private CoreAccountRepositoryPort accountRepository;
    @Mock
    private CoreTransactionRepositoryPort transactionRepository;
    @Mock
    private DistributeDepositUseCase distributeDepositUseCase;
    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private DepositUseCase depositUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldExecuteDeposit() {
        UUID userId = UUID.randomUUID();
        DepositRequest request = new DepositRequest("PSE", new BigDecimal("100000"));

        User user = new User();
        user.setId(userId);
        user.setKycStatus(KycStatus.APPROVED);

        CoreAccount account = new CoreAccount();
        account.setId(UUID.randomUUID());
        account.setUserId(userId);
        account.setStatus(com.cooperativa.met.domain.account.model.AccountStatus.ACTIVE);
        account.setPrincipalBalance(BigDecimal.ZERO);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(CoreAccount.class))).thenReturn(account);

        depositUseCase.execute(userId, request);

        verify(accountRepository, times(1)).save(any(CoreAccount.class));
        verify(transactionRepository, times(1)).save(any());
        verify(distributeDepositUseCase, times(1)).execute(any(), any(), any());
    }

    @Test
    void shouldThrowWhenAmountInvalid() {
        UUID userId = UUID.randomUUID();
        DepositRequest request = new DepositRequest("PSE", BigDecimal.ZERO);

        assertThrows(BusinessRuleException.class, () -> depositUseCase.execute(userId, request));
    }
}
