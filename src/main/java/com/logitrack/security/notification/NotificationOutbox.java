package com.logitrack.security.notification;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records welcome notifications in the transaction that creates the user.
 * @author dominox45
 */
@Component
public class NotificationOutbox {
    private final JdbcTemplate jdbc;
    private final boolean enabled;

    /** Configures outbox persistence and the registration feature flag. */
    public NotificationOutbox(JdbcTemplate jdbc, @Value("${mail.notifications.enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    /** Records one welcome message for an enabled registration. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueWelcome(UUID userId, String recipient) {
        if (enabled) {
            jdbc.update("""
                    INSERT INTO outbox_notifications (id, recipient, status, next_attempt_at)
                    VALUES (?, ?, 'PENDING', ?)
                    """, userId.toString(), recipient, Timestamp.from(Instant.now()));
        }
    }
}
