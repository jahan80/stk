package com.starterkit.notif.notif.infrastructure.provider;

import com.starterkit.notif.notif.domain.provider.EmailProvider;
import com.starterkit.notif.notif.domain.provider.ProviderResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "notif.provider.email",
        havingValue = "smtp"
)
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${notif.email.from:noreply@starterkit.local}")
    private String from;

    @Override
    public ProviderResponse send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "(No Subject)");
            helper.setText(body, isHtml(body));

            mailSender.send(message);

            String messageId = message.getMessageID();
            if (messageId == null) {
                messageId = "smtp-" + UUID.randomUUID();
            }

            log.info("Email sent via SMTP: to={}, subject={}, messageId={}",
                    to, subject, messageId);

            return ProviderResponse.success(messageId);

        } catch (Exception ex) {
            log.error("Failed to send email via SMTP: to={}, subject={}",
                    to, subject, ex);
            return ProviderResponse.failure(ex.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "SMTP";
    }

    private boolean isHtml(String body) {
        if (body == null) return false;
        String lower = body.toLowerCase();
        return lower.contains("<html") || lower.contains("<body") || lower.contains("<p>");
    }
}
