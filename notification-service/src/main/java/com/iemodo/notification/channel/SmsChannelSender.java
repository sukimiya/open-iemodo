package com.iemodo.notification.channel;

import com.iemodo.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * SMS channel — stub implementation.
 *
 * <p>To activate: inject Twilio SDK or 阿里云短信 client and implement {@link #send}.
 * The rest of the pipeline (template rendering, record persistence, retry) needs no changes.
 */
@Slf4j
@Component
public class SmsChannelSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public Mono<Void> send(String recipient, String subject, String body) {
        // TODO: integrate Twilio or 阿里云短信
        log.warn("SMS channel not yet implemented — skipping send to {}", recipient);
        return Mono.empty();
    }
}
