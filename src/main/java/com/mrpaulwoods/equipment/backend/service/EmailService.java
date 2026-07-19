package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import com.mrpaulwoods.equipment.backend.util.DueStatus;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// Deliberately not @Transactional: SMTP sends are slow external I/O and must not
// hold a DB connection. DashboardService does its reads in its own read-only transaction.
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final DashboardService dashboardService;
    private final AppProperties appProperties;

    public void sendPasswordResetEmail(String toEmail, String token) throws Exception {
        String resetUrl = appProperties.getFrontendUrl() + "/reset-password?token=" + token;

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Password Reset Request</h2>
                    <p>You requested a password reset for your Equipment App account.</p>
                    <p>Click the button below to reset your password. This link expires in 1 hour.</p>
                    <p style="text-align: center; margin: 24px 0;">
                        <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">Reset Password</a>
                    </p>
                    <p>If the button does not work, copy and paste this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p>If you did not request this, you can safely ignore this email.</p>
                </div>
                """.formatted(resetUrl, resetUrl, resetUrl);

        String text = "You requested a password reset for your Equipment App account.\n\n" +
                      "Copy and paste this link into your browser (expires in 1 hour):\n" + resetUrl + "\n\n" +
                      "If you did not request this, you can safely ignore this email.";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(appProperties.getSmtpFrom());
        helper.setTo(toEmail);
        helper.setSubject("Password reset request for Equipment App");
        helper.setText(text, html);
        mailSender.send(message);
        log.info("Password reset email sent to {}", toEmail);
    }

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
        String color = item.status() == DueStatus.OVERDUE ? "red" : "inherit";
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
