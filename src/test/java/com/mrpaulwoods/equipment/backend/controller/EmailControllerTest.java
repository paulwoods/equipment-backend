package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.exception.GlobalExceptionHandler;
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
        // Real GlobalExceptionHandler wired in (not a mock/stub) so failure tests prove
        // the actual RFC7807 problem-detail pipeline the try/catch used to bypass.
        mockMvc = MockMvcBuilders.standaloneSetup(emailController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void sendDashboard_whenSucceeds_returnsSuccess() throws Exception {
        doNothing().when(emailService).sendDashboardEmail();

        mockMvc.perform(post("/api/v1/email/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sendDashboard_whenServiceThrows_returnsRfc7807ProblemDetail() throws Exception {
        doThrow(new RuntimeException("SMTP connection failed")).when(emailService).sendDashboardEmail();

        mockMvc.perform(post("/api/v1/email/dashboard"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.instance").value("/api/v1/email/dashboard"));
    }

    @Test
    void sendDashboard_whenServiceThrowsWithNullMessage_stillReturnsRfc7807ProblemDetail() throws Exception {
        doThrow(new RuntimeException((String) null)).when(emailService).sendDashboardEmail();

        mockMvc.perform(post("/api/v1/email/dashboard"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
