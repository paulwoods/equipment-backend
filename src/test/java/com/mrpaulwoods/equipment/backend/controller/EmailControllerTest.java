package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailController emailController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(emailController).build();
    }

    @Test
    void sendDashboard_whenSucceeds_returnsSuccess() throws Exception {
        doNothing().when(emailService).sendDashboardEmail();

        mockMvc.perform(post("/api/v1/email/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendDashboard_whenServiceThrows_returns500WithError() throws Exception {
        doThrow(new RuntimeException("SMTP connection failed")).when(emailService).sendDashboardEmail();

        mockMvc.perform(post("/api/v1/email/dashboard"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("SMTP connection failed"));
    }

    @Test
    void sendDashboard_whenServiceThrowsWithNullMessage_returns500WithFallback() throws Exception {
        doThrow(new RuntimeException((String) null)).when(emailService).sendDashboardEmail();

        mockMvc.perform(post("/api/v1/email/dashboard"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to send email"));
    }
}
