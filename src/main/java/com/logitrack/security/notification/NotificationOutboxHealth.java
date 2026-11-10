package com.logitrack.security.notification;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reports database access and aggregate outbox counts without exposing recipients.
 * @author dominox45
 */
@Component("notificationOutboxHealthIndicator")
@ConditionalOnProperty(name = "mail.notifications.enabled", havingValue = "true")
class NotificationOutboxHealth implements HealthIndicator {
    private final JdbcTemplate jdbc;

    NotificationOutboxHealth(JdbcTemplate jdbc, MeterRegistry metrics) {
        this.jdbc = jdbc;
        metrics.gauge("logitrack.notification.outbox.pending", this, outbox -> outbox.count("PENDING"));
        metrics.gauge("logitrack.notification.outbox.failed", this, outbox -> outbox.count("FAILED"));
    }

    /**
     * Checks outbox persistence and reports aggregate counts independently of SMTP.
     * @return outbox health
     */
    @Override
    public Health health() {
        try {
            return Health.up().withDetail("pending", count("PENDING"))
                    .withDetail("failed", count("FAILED")).build();
        } catch (DataAccessException unavailable) {
            return Health.down().build();
        }
    }

    private long count(String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM outbox_notifications WHERE status = ?", Long.class, status);
    }
}
