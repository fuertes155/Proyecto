package com.cooperativa.met.infrastructure.web.notification;

import com.cooperativa.met.application.notification.dto.NotificationResponse;
import com.cooperativa.met.application.notification.usecase.GetNotificationsUseCase;
import com.cooperativa.met.application.notification.usecase.GetUnreadNotificationsCountUseCase;
import com.cooperativa.met.application.notification.usecase.MarkNotificationReadUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetNotificationsUseCase getNotificationsUseCase;

    @MockBean
    private GetUnreadNotificationsCountUseCase getUnreadNotificationsCountUseCase;

    @MockBean
    private MarkNotificationReadUseCase markNotificationReadUseCase;

    private UsernamePasswordAuthenticationToken getAuth() {
        return new UsernamePasswordAuthenticationToken(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), null, List.of());
    }

    @Test
    void shouldGetNotifications() throws Exception {
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(),
                "TRANSFER",
                "New Transfer",
                "You received a transfer",
                false,
                LocalDateTime.now(),
                null
        );

        Mockito.when(getNotificationsUseCase.execute(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/notifications")
                .with(authentication(getAuth()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("New Transfer"));
    }

    @Test
    void shouldGetUnreadCount() throws Exception {
        Mockito.when(getUnreadNotificationsCountUseCase.execute(any(UUID.class))).thenReturn(5);

        mockMvc.perform(get("/v1/notifications/unread-count")
                .with(authentication(getAuth()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void shouldMarkAsRead() throws Exception {
        UUID notificationId = UUID.randomUUID();

        Mockito.doNothing().when(markNotificationReadUseCase).execute(any(UUID.class), any(UUID.class));

        mockMvc.perform(put("/v1/notifications/" + notificationId + "/read")
                .with(authentication(getAuth()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
