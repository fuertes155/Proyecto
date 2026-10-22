package com.cooperativa.met.application.compliance.service;

import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceBlockServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private ComplianceBlockService complianceBlockService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldBlockUserAndRejectTheirKyc() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId).status(UserStatus.ACTIVE).kycStatus(KycStatus.PENDING).build()));

        complianceBlockService.blockUser(userId);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(UserStatus.BLOCKED, saved.getValue().getStatus());
        assertEquals(KycStatus.REJECTED, saved.getValue().getKycStatus());
    }

    @Test
    void shouldDoNothingForUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        complianceBlockService.blockUser(userId);

        verify(userRepository, never()).save(any());
    }
}
