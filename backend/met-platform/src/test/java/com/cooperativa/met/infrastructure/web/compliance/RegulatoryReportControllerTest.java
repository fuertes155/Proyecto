package com.cooperativa.met.infrastructure.web.compliance;

import com.cooperativa.met.application.compliance.dto.GenerateReportRequest;
import com.cooperativa.met.application.compliance.dto.RegulatoryReportResponse;
import com.cooperativa.met.application.compliance.usecase.DownloadRegulatoryReportUseCase;
import com.cooperativa.met.application.compliance.usecase.GenerateSupersolidariaReportUseCase;
import com.cooperativa.met.application.compliance.usecase.GetRegulatoryReportUseCase;
import com.cooperativa.met.application.compliance.usecase.ListRegulatoryReportsUseCase;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class RegulatoryReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerateSupersolidariaReportUseCase generateUseCase;
    @MockBean
    private ListRegulatoryReportsUseCase listUseCase;
    @MockBean
    private GetRegulatoryReportUseCase getUseCase;
    @MockBean
    private DownloadRegulatoryReportUseCase downloadUseCase;

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldListReportTypes() throws Exception {
        mockMvc.perform(get("/v1/compliance/reports/types")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists());
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldGenerateReport() throws Exception {
        RegulatoryReportResponse response = new RegulatoryReportResponse(
                UUID.randomUUID(),
                SupersolidariaReportType.ASOCIADOS.name(),
                2026,
                7,
                LocalDateTime.now(),
                "PENDING"
        );

        Mockito.when(generateUseCase.execute(any(UUID.class), any(GenerateReportRequest.class))).thenReturn(response);

        String json = "{\"reportType\": \"ASOCIADOS\", \"year\": 2026, \"month\": 7}";

        mockMvc.perform(post("/v1/compliance/reports/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void shouldListReports() throws Exception {
        RegulatoryReportResponse response = new RegulatoryReportResponse(
                UUID.randomUUID(),
                SupersolidariaReportType.ASOCIADOS.name(),
                2026,
                7,
                LocalDateTime.now(),
                "COMPLETED"
        );

        Mockito.when(listUseCase.execute(eq(2026), eq(7))).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/compliance/reports?year=2026&month=7")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
