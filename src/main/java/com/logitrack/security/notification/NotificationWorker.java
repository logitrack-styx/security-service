package com.logitrack.security.notification;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Delivers committed notifications in bounded batches with leased claims and retries.
 * @author dominox45
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mail.notifications.enabled", havingValue = "true")
class NotificationWorker {
    private static final int MAX_ATTEMPTS = 5;
    private final JdbcTemplate jdbc;
    private final NotificationSender sender;

    @Scheduled(fixedDelayString = "${mail.notifications.poll-delay-ms:5000}",
            initialDelayString = "${mail.notifications.poll-delay-ms:5000}")
    void deliverDueNotifications() {
        var now = Timestamp.from(Instant.now());
        jdbc.update("""
                UPDATE outbox_notifications SET status = 'FAILED', lease_token = NULL
                WHERE status = 'PENDING' AND attempts >= ? AND next_attempt_at <= ?
                """, MAX_ATTEMPTS, now);
        var due = jdbc.query("""
                SELECT id, recipient, attempts FROM outbox_notifications
                WHERE status = 'PENDING' AND next_attempt_at <= ? AND attempts < ?
                ORDER BY next_attempt_at, id LIMIT 20
                """, (row, index) -> new Notification(row.getString("id"),
                row.getString("recipient"), row.getInt("attempts")), now, MAX_ATTEMPTS);
        due.forEach(this::deliver);
    }

    private void deliver(Notification notification) {
        var token = UUID.randomUUID().toString();
        var now = Instant.now();
        int claimed = jdbc.update("""
                UPDATE outbox_notifications
                SET lease_token = ?, attempts = attempts + 1, next_attempt_at = ?
                WHERE id = ? AND status = 'PENDING' AND attempts = ? AND next_attempt_at <= ?
                """, token, Timestamp.from(now.plusSeconds(120)), notification.id(),
                notification.attempts(), Timestamp.from(now));
        if (claimed == 0) {
            return;
        }
        try {
            sender.sendWelcome(notification.id(), notification.recipient());
        } catch (MailException failure) {
            int attempt = notification.attempts() + 1;
            String status = attempt == MAX_ATTEMPTS ? "FAILED" : "PENDING";
            finish(notification.id(), token, status, Instant.now().plusSeconds(15L << (attempt - 1)));
            log.warn("Welcome notification {} attempt {} ended in {}", notification.id(), attempt, status);
            return;
        }
        finish(notification.id(), token, "SENT", Instant.now());
        log.info("Welcome notification {} delivered", notification.id());
    }

    private void finish(String id, String token, String status, Instant nextAttempt) {
        jdbc.update("""
                UPDATE outbox_notifications SET status = ?, next_attempt_at = ?, lease_token = NULL
                WHERE id = ? AND lease_token = ?
                """, status, Timestamp.from(nextAttempt), id, token);
    }

    private record Notification(String id, String recipient, int attempts) {}
}
