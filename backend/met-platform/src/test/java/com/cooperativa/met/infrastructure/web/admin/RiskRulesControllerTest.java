package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.RiskRuleRequest;
import com.cooperativa.met.application.admin.usecase.ManageRiskRulesUseCase;
import com.cooperativa.met.domain.admin.model.RiskRule;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class RiskRulesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManageRiskRulesUseCase manageRiskRulesUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldGetAllRiskRules() throws Exception {
        RiskRule rule = new RiskRule();
        rule.setId(UUID.randomUUID());
        rule.setRuleName("MAX_DAILY_AMOUNT");

        Mockito.when(manageRiskRulesUseCase.getAll()).thenReturn(List.of(rule));

        mockMvc.perform(get("/v1/admin/risk-rules")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleName").value("MAX_DAILY_AMOUNT"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldCreateRiskRule() throws Exception {
        RiskRule rule = new RiskRule();
        rule.setId(UUID.randomUUID());
        rule.setRuleName("MAX_TX");

        Mockito.when(manageRiskRulesUseCase.create(any(UUID.class), any(RiskRuleRequest.class), anyString())).thenReturn(rule);

        String json = "{\"ruleName\": \"MAX_TX\", \"ruleCondition\": \"amount > 5000000\", \"action\": \"FLAG_REVIEW\", \"description\": \"Test\"}";

        mockMvc.perform(post("/v1/admin/risk-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleName").value("MAX_TX"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = {"ADMIN"})
    void shouldToggleRiskRule() throws Exception {
        UUID ruleId = UUID.randomUUID();
        RiskRule rule = new RiskRule();
        rule.setId(ruleId);
        rule.setActivo(true);

        Mockito.when(manageRiskRulesUseCase.toggleActivo(any(UUID.class), any(UUID.class), anyBoolean(), anyString())).thenReturn(rule);

        mockMvc.perform(patch("/v1/admin/risk-rules/" + ruleId + "/toggle?activo=true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }
}
