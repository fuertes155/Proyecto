package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.VerifyRecipientResponse;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class VerifyRecipientUseCaseTest {

    @Mock
    private CoreAccountRepositoryPort accountRepository;
    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private VerifyRecipientUseCase verifyRecipientUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldVerifyRecipientByAccountNumber() {
        UUID currentUserId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        String accountNumber = "1234567890";

        CoreAccount account = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(recipientUserId)
                .accountNumber(accountNumber)
                .principalBalance(BigDecimal.ZERO)
                .interestBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(1L)
                .build();

        User owner = User.builder()
                .id(recipientUserId)
                .documentType(DocumentType.CC)
                .documentNumber("654321")
                .email("owner@test.com")
                .phone("3009876543")
                .firstName("John")
                .lastName("Doe")
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
                .paymentCardToken("")
                .lastKnownDeviceId("")
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(userRepository.findById(recipientUserId)).thenReturn(Optional.of(owner));

        VerifyRecipientResponse response = verifyRecipientUseCase.execute(accountNumber, currentUserId);

        assertEquals("John Doe", response.name());
        assertEquals(account.getId(), response.accountId());
    }

    @Test
    void shouldThrowWhenSameAccount() {
        UUID currentUserId = UUID.randomUUID();
        String accountNumber = "1234567890";

        CoreAccount account = CoreAccount.builder()
                .id(UUID.randomUUID())
                .userId(currentUserId)
                .accountNumber(accountNumber)
                .principalBalance(BigDecimal.ZERO)
                .interestBalance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(1L)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(BusinessRuleException.class, () -> verifyRecipientUseCase.execute(accountNumber, currentUserId));
    }
}
