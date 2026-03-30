package com.mrpaulwoods.equipment.backend.scheduler;

import com.mrpaulwoods.equipment.backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MaintenanceSchedulerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MaintenanceScheduler maintenanceScheduler;

    @Test
    void sendWeeklyDashboardEmail_invokesEmailService() throws Exception {
        maintenanceScheduler.sendWeeklyDashboardEmail();

        verify(emailService).sendDashboardEmail();
    }

    @Test
    void sendWeeklyDashboardEmail_whenEmailServiceThrows_doesNotPropagate() throws Exception {
        doThrow(new Exception("SMTP failure")).when(emailService).sendDashboardEmail();

        // should not throw — exception is caught and logged
        maintenanceScheduler.sendWeeklyDashboardEmail();

        verify(emailService).sendDashboardEmail();
    }
}
