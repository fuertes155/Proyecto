package com.cooperativa.met.infrastructure.web.solidarity;

import com.cooperativa.met.application.solidarity.dto.CreateSolidarityGroupRequest;
import com.cooperativa.met.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.met.application.solidarity.usecase.ContributeToPoolUseCase;
import com.cooperativa.met.application.solidarity.usecase.CreateSolidarityGroupUseCase;
import com.cooperativa.met.application.solidarity.usecase.GetSolidarityGroupUseCase;
import com.cooperativa.met.application.solidarity.usecase.JoinSolidarityGroupUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListGroupLoansUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListGroupMembersUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListLoanInstallmentsUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListMySolidarityGroupsUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListPoolTransactionsUseCase;
import com.cooperativa.met.application.solidarity.usecase.RepayInstallmentUseCase;
import com.cooperativa.met.application.solidarity.usecase.RequestMicroLoanUseCase;
import com.cooperativa.met.application.solidarity.usecase.ReviewMicroLoanUseCase;
import com.cooperativa.met.domain.solidarity.model.GroupStatus;
import com.cooperativa.met.domain.solidarity.model.MemberRole;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class SolidarityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CreateSolidarityGroupUseCase createGroupUseCase;
    @MockBean private JoinSolidarityGroupUseCase joinGroupUseCase;
    @MockBean private ListMySolidarityGroupsUseCase listGroupsUseCase;
    @MockBean private GetSolidarityGroupUseCase getGroupUseCase;
    @MockBean private ListGroupMembersUseCase listMembersUseCase;
    @MockBean private ContributeToPoolUseCase contributeUseCase;
    @MockBean private RequestMicroLoanUseCase requestLoanUseCase;
    @MockBean private ReviewMicroLoanUseCase reviewLoanUseCase;
    @MockBean private RepayInstallmentUseCase repayInstallmentUseCase;
    @MockBean private ListGroupLoansUseCase listLoansUseCase;
    @MockBean private ListLoanInstallmentsUseCase listInstallmentsUseCase;
    @MockBean private ListPoolTransactionsUseCase listTransactionsUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldCreateGroup() throws Exception {
        SolidarityGroupResponse response = new SolidarityGroupResponse(
                UUID.randomUUID(),
                "Fondo de Vecinos",
                "Fondo para emergencias del barrio",
                "INVITECODE",
                new BigDecimal("10000"),
                new BigDecimal("50"),
                new BigDecimal("0.05"),
                new BigDecimal("1000000"),
                new BigDecimal("500000"),
                10,
                50,
                GroupStatus.ACTIVE,
                MemberRole.ADMIN,
                Instant.now()
        );

        Mockito.when(createGroupUseCase.execute(any(UUID.class), any(CreateSolidarityGroupRequest.class))).thenReturn(response);

        String json = "{\"name\": \"Fondo de Vecinos\", \"description\": \"Fondo para emergencias del barrio\"}";

        mockMvc.perform(post("/v1/solidarity/groups").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fondo de Vecinos"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldListGroups() throws Exception {
        SolidarityGroupResponse response = new SolidarityGroupResponse(
                UUID.randomUUID(),
                "Fondo de Vecinos",
                "Fondo para emergencias del barrio",
                "INVITECODE",
                new BigDecimal("10000"),
                new BigDecimal("50"),
                new BigDecimal("0.05"),
                new BigDecimal("1000000"),
                new BigDecimal("500000"),
                10,
                50,
                GroupStatus.ACTIVE,
                MemberRole.MEMBER,
                Instant.now()
        );

        Mockito.when(listGroupsUseCase.execute(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/solidarity/groups")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fondo de Vecinos"));
    }
}
