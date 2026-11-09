package com.logitrack.security.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises outbox rollback, retry, delivery and terminal failure against a database.
 * @author dominox45
 */
class NotificationWorkerTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "MAILPIT_SMOKE_PORT", matches = "[0-9]+")
    void deliversWelcomeToLocalMailpit() throws Exception {
        var smtp = new JavaMailSenderImpl();
        smtp.setHost("127.0.0.1");
        smtp.setPort(Integer.parseInt(System.getenv("MAILPIT_SMOKE_PORT")));
        smtp.getJavaMailProperties().setProperty("mail.smtp.timeout", "5000");
        new SmtpNotificationSender(smtp, "no-reply@logitrack.invalid")
                .sendWelcome("security-mailpit-smoke", "smoke@example.invalid");
    }

    @Test
    void retriesCommittedNotificationsAndDoesNotResendTerminalRows() {
        var database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        try {
            new ResourceDatabasePopulator(new ClassPathResource("db/migration/V2__notification_outbox.sql"))
                    .execute(database);
            var jdbc = new JdbcTemplate(database);
            var outbox = new NotificationOutbox(jdbc, true);
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(database));
            var sender = mock(NotificationSender.class);
            var worker = new NotificationWorker(jdbc, sender);

            transaction.executeWithoutResult(status -> {
                outbox.enqueueWelcome(UUID.randomUUID(), "rollback@example.invalid");
                status.setRollbackOnly();
            });
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM outbox_notifications", Integer.class));

            var id = UUID.randomUUID();
            transaction.executeWithoutResult(status -> outbox.enqueueWelcome(id, "test@example.invalid"));
            doThrow(new MailSendException("SMTP unavailable")).doNothing()
                    .when(sender).sendWelcome(id.toString(), "test@example.invalid");
            worker.deliverDueNotifications();
            assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM outbox_notifications", String.class));
            worker.deliverDueNotifications();
            verify(sender).sendWelcome(id.toString(), "test@example.invalid");

            jdbc.update("UPDATE outbox_notifications SET next_attempt_at = ?", Timestamp.from(Instant.now()));
            worker.deliverDueNotifications();
            worker.deliverDueNotifications();
            assertEquals("SENT", jdbc.queryForObject("SELECT status FROM outbox_notifications", String.class));
            verify(sender, times(2)).sendWelcome(id.toString(), "test@example.invalid");

            var failed = UUID.randomUUID();
            transaction.executeWithoutResult(status -> outbox.enqueueWelcome(failed, "fail@example.invalid"));
            doThrow(new MailSendException("SMTP unavailable")).when(sender).sendWelcome(anyString(),
                    org.mockito.ArgumentMatchers.eq("fail@example.invalid"));
            jdbc.update("UPDATE outbox_notifications SET attempts = 4 WHERE id = ?", failed.toString());
            worker.deliverDueNotifications();
            worker.deliverDueNotifications();
            assertEquals("FAILED", jdbc.queryForObject("SELECT status FROM outbox_notifications WHERE id = ?",
                    String.class, failed.toString()));
            verify(sender).sendWelcome(failed.toString(), "fail@example.invalid");
        } finally {
            database.shutdown();
        }
    }
}
