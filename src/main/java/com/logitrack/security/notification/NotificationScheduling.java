package com.logitrack.security.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the outbox scheduler when welcome notifications are enabled.
 * @author dominox45
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "mail.notifications.enabled", havingValue = "true")
class NotificationScheduling {}
