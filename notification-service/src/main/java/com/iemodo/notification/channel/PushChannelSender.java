package com.iemodo.notification.channel;

import com.iemodo.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Push notification channel — stub implementation.
 *
 * <p>To activate: inject Firebase Admin SDK and implement {@link #send}.
 * recipient = FCM device token (stored in user_devices.fcm_token).
 */
@Slf4j
@Component
public class PushChannelSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public Mono<Void> send(String recipient, String subject, String body) {
        // TODO: integrate Firebase Admin SDK
        log.warn("Push channel not yet implemented — skipping send to device={}", recipient);
        return Mono.empty();
    }
}
