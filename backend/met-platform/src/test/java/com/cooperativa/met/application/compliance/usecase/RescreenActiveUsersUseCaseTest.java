package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescreenActiveUsersUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private ComplianceCheckPort complianceCheckPort;

    @InjectMocks
    private RescreenActiveUsersUseCase rescreenActiveUsersUseCase;

    @Test
    void shouldScreenEveryActiveUserAgainstOfacAndOnuAndCountNewMatches() {
        User clean = user(UserStatus.ACTIVE);
        User sanctioned = user(UserStatus.ACTIVE);
        when(userRepository.findAll()).thenReturn(List.of(clean, sanctioned));
        when(complianceCheckPort.checkUser(eq(clean.getId()), any())).thenReturn(ComplianceResult.CLEAR);
        when(complianceCheckPort.checkUser(sanctioned.getId(), ComplianceListType.OFAC)).thenReturn(ComplianceResult.MATCH);
        when(complianceCheckPort.checkUser(sanctioned.getId(), ComplianceListType.ONU)).thenReturn(ComplianceResult.CLEAR);

        int newMatches = rescreenActiveUsersUseCase.execute();

        assertEquals(1, newMatches);
        verify(complianceCheckPort).persistCheck(sanctioned.getId(), ComplianceListType.OFAC, ComplianceResult.MATCH,
                "{\"context\":\"PERIODIC_RESCREEN\"}");
        verify(complianceCheckPort).persistCheck(clean.getId(), ComplianceListType.ONU, ComplianceResult.CLEAR,
                "{\"context\":\"PERIODIC_RESCREEN\"}");
    }

    @Test
    void shouldSkipAccountsThatAreNotActive() {
        when(userRepository.findAll()).thenReturn(List.of(user(UserStatus.BLOCKED), user(UserStatus.PENDING_VERIFICATION)));

        assertEquals(0, rescreenActiveUsersUseCase.execute());

        verify(complianceCheckPort, never()).checkUser(any(), any());
    }

    private User user(UserStatus status) {
        return User.builder().id(UUID.randomUUID()).status(status).build();
    }
}
