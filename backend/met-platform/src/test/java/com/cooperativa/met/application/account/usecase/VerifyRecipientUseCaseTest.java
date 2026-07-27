package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.VerifyRecipientResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

        CoreAccount account = new CoreAccount();
        account.setId(UUID.randomUUID());
        account.setUserId(recipientUserId);

        User owner = new User();
        owner.setId(recipientUserId);
        owner.setFirstName("John");
        owner.setLastName("Doe");

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

        CoreAccount account = new CoreAccount();
        account.setId(UUID.randomUUID());
        account.setUserId(currentUserId);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(BusinessRuleException.class, () -> verifyRecipientUseCase.execute(accountNumber, currentUserId));
    }
}
