package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailService {

    private final JavaMailSender mailSender;
    private final DashboardService dashboardService;
    private final AppProperties appProperties;

    public void sendDashboardEmail() throws Exception {
        List<DashboardItem> items = dashboardService.getDashboardItems();

        String tableRows = items.stream()
                .map(this::buildTableRow)
                .reduce("", String::concat);

        String html = buildHtml(tableRows);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(appProperties.getSmtpFrom());
        helper.setTo(appProperties.getEmailRecipient());
        helper.setSubject("Equipment Maintenance Dashboard - " + LocalDate.now());
        helper.setText(html, true);
        mailSender.send(message);
        log.info("Dashboard email sent to {}", appProperties.getEmailRecipient());
    }

    private String buildTableRow(DashboardItem item) {
        String color = "OVERDUE".equals(item.status()) ? "red" : "inherit";
        String daysTillDue = item.daysTillDue() != null ? String.valueOf(item.daysTillDue()) : "N/A";
        String dueDate = item.dueDate() != null ? item.dueDate() : "N/A";
        return """
                <tr style="border-bottom: 1px solid #ddd;">
                    <td style="padding: 8px;">%s</td>
                    <td style="padding: 8px;">%s</td>
                    <td style="padding: 8px;">%d</td>
                    <td style="padding: 8px; color: %s">%s</td>
                    <td style="padding: 8px;">%s</td>
                    <td style="padding: 8px;">%s</td>
                </tr>
                """.formatted(item.equipmentName(), item.procedureName(),
                item.intervalDays(), color, daysTillDue, dueDate, item.status());
    }

    private String buildHtml(String tableRows) {
        String appUrl = appProperties.getAppUrl();
        return """
                <h1>Equipment Maintenance Dashboard</h1>
                <p>Current dashboard status as of %s</p>
                <p><a href="%s">View Dashboard</a></p>
                <table style="width: 100%%; border-collapse: collapse;">
                    <thead>
                        <tr style="background-color: #f2f2f2; text-align: left;">
                            <th style="padding: 8px;">Equipment</th>
                            <th style="padding: 8px;">Procedure</th>
                            <th style="padding: 8px;">Interval (Days)</th>
                            <th style="padding: 8px;">Days Till Due</th>
                            <th style="padding: 8px;">Due Date</th>
                            <th style="padding: 8px;">Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>
                """.formatted(LocalDate.now(), appUrl, tableRows);
    }
}
