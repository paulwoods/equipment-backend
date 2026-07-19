package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import com.mrpaulwoods.equipment.backend.util.DueStatus;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        when(appProperties.getSmtpFrom()).thenReturn("noreply@example.com");
        when(appProperties.getEmailRecipient()).thenReturn("user@example.com");
        when(appProperties.getAppUrl()).thenReturn("http://localhost:8080");
    }

    private MimeMessage mockMimeMessage() {
        MimeMessage msg = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(msg);
        return msg;
    }

    @Test
    void sendDashboardEmail_sendsToConfiguredRecipient() throws Exception {
        mockMimeMessage();
        when(dashboardService.getDashboardItems()).thenReturn(List.of());

        emailService.sendDashboardEmail();

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendDashboardEmail_withEmptyDashboard_stillSends() throws Exception {
        mockMimeMessage();
        when(dashboardService.getDashboardItems()).thenReturn(List.of());

        emailService.sendDashboardEmail();

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendDashboardEmail_whenMailSenderThrows_propagatesException() {
        MimeMessage msg = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(msg);
        when(dashboardService.getDashboardItems()).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendDashboardEmail())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP error");
    }

    @Test
    void sendDashboardEmail_withOverdueItem_createsMimeMessage() throws Exception {
        mockMimeMessage();
        DashboardItem item = new DashboardItem(
                "eq-1", "Acme X100", "proc-1", "Oil Change",
                "Change oil", 30, -5, LocalDate.now().minusDays(5).toString(), DueStatus.OVERDUE
        );
        when(dashboardService.getDashboardItems()).thenReturn(List.of(item));

        emailService.sendDashboardEmail();

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendDashboardEmail_withItemHavingNullDaysTillDue_doesNotThrow() throws Exception {
        mockMimeMessage();
        DashboardItem item = new DashboardItem(
                "eq-1", "Acme X100", "proc-1", "Oil Change",
                "Change oil", 30, null, null, DueStatus.NO_HISTORY
        );
        when(dashboardService.getDashboardItems()).thenReturn(List.of(item));

        emailService.sendDashboardEmail();

        verify(mailSender).send(any(MimeMessage.class));
    }
}
