package com.mrpaulwoods.equipment.backend.scheduler;

import com.mrpaulwoods.equipment.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class MaintenanceScheduler {

    private final EmailService emailService;

    // Saturday at 7:00 AM America/Chicago (handles CST/CDT automatically)
    @Scheduled(cron = "0 0 7 * * SAT", zone = "America/Chicago")
    @SchedulerLock(name = "sendWeeklyDashboardEmail", lockAtLeastFor = "PT1M", lockAtMostFor = "PT15M")
    public void sendWeeklyDashboardEmail() {
        log.info("Running scheduled dashboard email (Saturday 7:00 AM Central Time)...");
        try {
            emailService.sendDashboardEmail();
            log.info("Scheduled dashboard email sent successfully.");
        } catch (Exception e) {
            log.error("Failed to send scheduled dashboard email", e);
        }
    }
}
