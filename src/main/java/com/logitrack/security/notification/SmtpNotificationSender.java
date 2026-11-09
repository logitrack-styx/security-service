package com.logitrack.security.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends welcome messages through the configured SMTP transport.
 * @author dominox45
 */
@Component
@ConditionalOnProperty(name = "mail.notifications.enabled", havingValue = "true")
class SmtpNotificationSender implements NotificationSender {
    private final JavaMailSender mail;
    private final String from;
    private final String template;

    SmtpNotificationSender(JavaMailSender mail, @Value("${mail.notifications.from}") String from) throws IOException {
        this.mail = mail;
        this.from = from;
        template = new ClassPathResource("mail/welcome.txt").getContentAsString(StandardCharsets.UTF_8);
    }

    /** {@inheritDoc} */
    @Override
    public void sendWelcome(String notificationId, String recipient) {
        mail.send(message -> {
            message.setFrom(from);
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, recipient);
            message.setSubject("Bienvenida a LogiTrack", StandardCharsets.UTF_8.name());
            message.setText(template, StandardCharsets.UTF_8.name());
            message.setHeader("X-LogiTrack-Notification-Id", notificationId);
        });
    }
}
