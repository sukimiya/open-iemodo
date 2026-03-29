package com.iemodo.notification.channel;

import com.iemodo.notification.domain.NotificationChannel;
import reactor.core.publisher.Mono;

/**
 * Strategy interface — one implementation per delivery channel.
 * New channels (WhatsApp, Line, WeChat…) just add an implementation.
 */
public interface ChannelSender {

    NotificationChannel channel();

    /**
     * Send a notification.
     *
     * @param recipient email / phone / device token
     * @param subject   subject line (may be null for SMS/Push)
     * @param body      rendered message body
     * @return empty Mono on success, error Mono on failure
     */
    Mono<Void> send(String recipient, String subject, String body);
}
