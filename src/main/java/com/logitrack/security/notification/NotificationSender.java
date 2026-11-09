package com.logitrack.security.notification;

/**
 * Delivers a persisted welcome notification.
 * @author dominox45
 */
public interface NotificationSender {
    /** Sends a welcome message identified by the stable notification ID. */
    void sendWelcome(String notificationId, String recipient);
}
