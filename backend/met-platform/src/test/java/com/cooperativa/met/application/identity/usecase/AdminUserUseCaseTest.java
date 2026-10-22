package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private AdminUserUseCase adminUserUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldChangeOnlyTheKycStatus() {
        User user = User.builder().id(userId).documentNumber("1086578123").kycStatus(KycStatus.PENDING).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = adminUserUseCase.updateKycStatus(userId, KycStatus.REJECTED);

        assertEquals(KycStatus.REJECTED, updated.getKycStatus());
        assertEquals("1086578123", updated.getDocumentNumber());
    }

    @Test
    void shouldFailForUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adminUserUseCase.updateKycStatus(userId, KycStatus.APPROVED));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        verify(userRepository, never()).save(any());
    }
}
