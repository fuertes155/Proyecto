package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.TransferRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.application.security.FraudDetectionService;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import com.cooperativa.met.infrastructure.security.IdempotencyService;
import com.cooperativa.met.application.account.service.TransferLimitService;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecuteTransferUseCaseTest {

    @Mock
    private CoreAccountRepositoryPort accountRepository;
    @Mock
    private CoreTransactionRepositoryPort transactionRepository;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private EncryptionPort encryptionPort;
    @Mock
    private OtpService otpService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private TransferLimitService transferLimitService;
    @Mock
    private PinAttemptService pinAttemptService;
    @Mock
    private FraudDetectionService fraudDetectionService;

    private ExecuteTransferUseCase executeTransferUseCase;

    private SimpleMeterRegistry meterRegistry;

    private UUID userId;
    private UUID destAccountId;
    private TransferRequest transferRequest;
    private User user;
    private CoreAccount sourceAccount;
    private CoreAccount destAccount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        destAccountId = UUID.randomUUID();
        
        transferRequest = new TransferRequest(
            destAccountId, 
            new BigDecimal("100.00"), 
            "Test transfer", 
            "1234", 
            "000000",
            "idem-1234"
        );

        user = User.builder()
                .id(userId)
                .documentNumber("123456789")
                .pinHash("hashed_pin")
                .kycStatus(com.cooperativa.met.domain.identity.model.KycStatus.APPROVED)
                .build();

        sourceAccount = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(AccountStatus.ACTIVE)
                .principalBalance(new BigDecimal("500.00"))
                .build();

        destAccount = CoreAccount.builder()
                .id(destAccountId)
                .userId(UUID.randomUUID())
                .status(AccountStatus.ACTIVE)
                .principalBalance(new BigDecimal("100.00"))
                .build();

        meterRegistry = new SimpleMeterRegistry();
        executeTransferUseCase = new ExecuteTransferUseCase(
            accountRepository, transactionRepository, userRepository,
            encryptionPort, otpService, auditLogService, idempotencyService,
            transferLimitService, pinAttemptService, fraudDetectionService, meterRegistry
        );
        try {
            java.lang.reflect.Method initMethod = ExecuteTransferUseCase.class.getDeclaredMethod("initMetrics");
            initMethod.setAccessible(true);
            initMethod.invoke(executeTransferUseCase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void execute_successfulTransfer() {
        // Arrange
        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(encryptionPort.decryptRsa("1234")).thenReturn("1234_decrypted");
        when(encryptionPort.verifyPin("1234_decrypted", "hashed_pin")).thenReturn(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccountId)).thenReturn(Optional.of(destAccount));
        when(otpService.validateOtp("123456789", "000000")).thenReturn(true);

        // Act
        executeTransferUseCase.execute(userId, transferRequest, "127.0.0.1");

        // Assert
        ArgumentCaptor<CoreAccount> accountCaptor = ArgumentCaptor.forClass(CoreAccount.class);
        verify(accountRepository, times(2)).save(accountCaptor.capture());
        
        CoreAccount savedSource = accountCaptor.getAllValues().get(0);
        CoreAccount savedDest = accountCaptor.getAllValues().get(1);
        
        assertEquals(new BigDecimal("400.00"), savedSource.getPrincipalBalance());
        assertEquals(new BigDecimal("200.00"), savedDest.getPrincipalBalance());
        
        verify(transactionRepository).save(any(CoreTransaction.class));
        verify(auditLogService).logSuccess(eq(userId), any(), any(), any(), any());
    }

    @Test
    void execute_failsWhenPinIsInvalid() {
        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(encryptionPort.decryptRsa("1234")).thenReturn("1234_decrypted");
        when(encryptionPort.verifyPin("1234_decrypted", "hashed_pin")).thenReturn(false);
        doThrow(new BusinessRuleException("INVALID_PIN", "Invalid pin")).when(pinAttemptService).checkAndRecordFailure(userId);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> executeTransferUseCase.execute(userId, transferRequest, "127.0.0.1"));
            
        assertEquals("INVALID_PIN", exception.getCode());
        verifyNoInteractions(accountRepository);
    }

    @Test
    void execute_failsWhenOtpIsInvalid() {
        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(encryptionPort.decryptRsa("1234")).thenReturn("1234_decrypted");
        when(encryptionPort.verifyPin("1234_decrypted", "hashed_pin")).thenReturn(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccountId)).thenReturn(Optional.of(destAccount));
        when(otpService.validateOtp("123456789", "000000")).thenReturn(false);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> executeTransferUseCase.execute(userId, transferRequest, "127.0.0.1"));
            
        assertEquals("INVALID_OTP", exception.getCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void execute_failsWhenSourceAccountInactive() {
        sourceAccount = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(AccountStatus.BLOCKED)
                .build();
                
        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(encryptionPort.decryptRsa("1234")).thenReturn("1234_decrypted");
        when(encryptionPort.verifyPin("1234_decrypted", "hashed_pin")).thenReturn(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(sourceAccount));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> executeTransferUseCase.execute(userId, transferRequest, "127.0.0.1"));
            
        assertEquals("INACTIVE_ACCOUNT", exception.getCode());
    }

    @Test
    void execute_failsWhenTransferringToSameAccount() {
        // Arrange: El destino es la misma cuenta origen
        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(encryptionPort.decryptRsa("1234")).thenReturn("1234_decrypted");
        when(encryptionPort.verifyPin("1234_decrypted", "hashed_pin")).thenReturn(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccountId)).thenReturn(Optional.of(sourceAccount)); // Devuelve la misma cuenta

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> executeTransferUseCase.execute(userId, transferRequest, "127.0.0.1"));
            
        assertEquals("SAME_ACCOUNT", exception.getCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void execute_failsWhenAmountIsNegativeOrZero() {
        // Arrange: Transferir cantidad negativa
        TransferRequest negativeRequest = new TransferRequest(
            destAccountId, 
            new BigDecimal("-5000.00"), 
            "Pago", 
            "1234", 
            "000000",
            "idem-5678"
        );
        
        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(encryptionPort.decryptRsa("1234")).thenReturn("1234_decrypted");
        when(encryptionPort.verifyPin("1234_decrypted", "hashed_pin")).thenReturn(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destAccountId)).thenReturn(Optional.of(destAccount));
        when(otpService.validateOtp("123456789", "000000")).thenReturn(true);

        // Act & Assert: Debe fallar al hacer debitPrincipal
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> executeTransferUseCase.execute(userId, negativeRequest, "127.0.0.1"));
            
        assertEquals("Amount must be greater than zero", exception.getMessage());
        verify(accountRepository, never()).save(any());
    }
}
