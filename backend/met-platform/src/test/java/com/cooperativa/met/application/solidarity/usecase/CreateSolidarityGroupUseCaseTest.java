package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.CreateSolidarityGroupRequest;
import com.cooperativa.met.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.solidarity.model.GroupStatus;
import com.cooperativa.met.domain.solidarity.model.MemberRole;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.model.SolidarityMember;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityMemberPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSolidarityGroupUseCaseTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private SolidarityGroupPort groupPort;
    @Mock private SolidarityMemberPort memberPort;
    @Mock private SolidarityMapper mapper;

    @InjectMocks
    private CreateSolidarityGroupUseCase useCase;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.builder().id(userId).status(UserStatus.ACTIVE).build();
    }

    @Test
    void execute_createsGroupSuccessfully_andAddsCreatorAsAdmin() {
        // Arrange
        CreateSolidarityGroupRequest request = new CreateSolidarityGroupRequest(
                "Grupo Familiar", "Ahorro grupal", null, null
        );
        SolidarityGroup savedGroup = SolidarityGroup.builder()
                .id(UUID.randomUUID()).name("Grupo Familiar")
                .status(GroupStatus.ACTIVE).poolBalance(BigDecimal.ZERO)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        SolidarityGroupResponse expectedResponse = mock(SolidarityGroupResponse.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(groupPort.existsByInviteCode(any())).thenReturn(false); // Código único disponible
        when(groupPort.save(any())).thenReturn(savedGroup);
        when(memberPort.save(any())).thenReturn(mock(SolidarityMember.class));
        when(mapper.toResponse(eq(savedGroup), eq(1), eq(MemberRole.ADMIN)))
                .thenReturn(expectedResponse);

        // Act
        SolidarityGroupResponse result = useCase.execute(userId, request);

        // Assert
        assertNotNull(result);
        // Verificar que el creador se agrega como ADMIN
        ArgumentCaptor<SolidarityMember> memberCaptor = ArgumentCaptor.forClass(SolidarityMember.class);
        verify(memberPort).save(memberCaptor.capture());
        assertEquals(MemberRole.ADMIN, memberCaptor.getValue().getRole());
        assertEquals(userId, memberCaptor.getValue().getUserId());
        assertEquals(BigDecimal.ZERO, memberCaptor.getValue().getTotalContributed());
    }

    @Test
    void execute_usesDefaultValues_whenMaxMembersAndContributionAreNull() {
        // Arrange: request sin minContribution ni maxMembers (usan defaults)
        CreateSolidarityGroupRequest request = new CreateSolidarityGroupRequest(
                "Grupo Test", "Desc", null, null
        );
        SolidarityGroup savedGroup = SolidarityGroup.builder()
                .id(UUID.randomUUID()).maxMembers(20)
                .minContribution(new BigDecimal("10000.00"))
                .status(GroupStatus.ACTIVE).poolBalance(BigDecimal.ZERO)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(groupPort.existsByInviteCode(any())).thenReturn(false);
        when(groupPort.save(any())).thenReturn(savedGroup);
        when(memberPort.save(any())).thenReturn(mock(SolidarityMember.class));
        when(mapper.toResponse(any(), anyInt(), any())).thenReturn(mock(SolidarityGroupResponse.class));

        // Act
        assertDoesNotThrow(() -> useCase.execute(userId, request));

        // Assert: el grupo guardado tiene los defaults correctos
        ArgumentCaptor<SolidarityGroup> groupCaptor = ArgumentCaptor.forClass(SolidarityGroup.class);
        verify(groupPort).save(groupCaptor.capture());
        assertEquals(20, groupCaptor.getValue().getMaxMembers());
        assertEquals(new BigDecimal("10000.00"), groupCaptor.getValue().getMinContribution());
    }

    @Test
    void execute_regeneratesInviteCode_whenCodeAlreadyExists() {
        // Arrange: el primer código generado ya existe, el segundo no
        CreateSolidarityGroupRequest request = new CreateSolidarityGroupRequest(
                "Grupo Código Colision", "Desc", null, null
        );
        SolidarityGroup savedGroup = SolidarityGroup.builder()
                .id(UUID.randomUUID()).status(GroupStatus.ACTIVE)
                .poolBalance(BigDecimal.ZERO).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        // Primera llamada: existe; segunda: no existe
        when(groupPort.existsByInviteCode(any()))
                .thenReturn(true)
                .thenReturn(false);
        when(groupPort.save(any())).thenReturn(savedGroup);
        when(memberPort.save(any())).thenReturn(mock(SolidarityMember.class));
        when(mapper.toResponse(any(), anyInt(), any())).thenReturn(mock(SolidarityGroupResponse.class));

        // Act & Assert
        assertDoesNotThrow(() -> useCase.execute(userId, request));
        // Se llamó a existsByInviteCode 2 veces (colisión + nuevo intento)
        verify(groupPort, times(2)).existsByInviteCode(any());
    }

    @Test
    void execute_throwsUserNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, new CreateSolidarityGroupRequest("G", "D", null, null)));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        verifyNoInteractions(groupPort, memberPort);
    }

    @Test
    void execute_throwsUserNotActive_whenUserIsPending() {
        User pendingUser = User.builder().id(userId).status(UserStatus.PENDING_VERIFICATION).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, new CreateSolidarityGroupRequest("G", "D", null, null)));

        assertEquals("USER_NOT_ACTIVE", ex.getCode());
        verifyNoInteractions(groupPort, memberPort);
    }
}
