package com.iemodo.notification.service;

import com.iemodo.notification.channel.ChannelSender;
import com.iemodo.notification.config.ChannelSenderRegistry;
import com.iemodo.notification.domain.NotificationRecord;
import com.iemodo.notification.repository.NotificationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Scheduled job that retries FAILED notifications.
 *
 * <p>Runs every 5 minutes. Retries up to {@link #MAX_RETRIES} times, then
 * marks the record with a dead-letter note (leaves status as FAILED so it
 * can be inspected via the admin API).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 50;

    private final NotificationRecordRepository recordRepository;
    private final TemplateService templateService;
    private final ChannelSenderRegistry senderRegistry;

    @Scheduled(fixedDelay = 300_000)
    public void retryFailedNotifications() {
        recordRepository.findFailedForRetry(MAX_RETRIES, BATCH_SIZE)
                .flatMap(this::retryOne)
                .doOnComplete(() -> log.debug("Notification retry cycle complete"))
                .subscribe(
                        null,
                        ex -> log.error("Notification retry scheduler error", ex)
                );
    }

    private Mono<Void> retryOne(NotificationRecord record) {
        String channelStr = record.getChannel() != null ? record.getChannel().name() : "EMAIL";
        String typeStr = record.getType() != null ? record.getType().name() : "GENERAL";
        int attempt = (record.getRetryCount() != null ? record.getRetryCount() : 0) + 1;

        log.info("Retrying notification id={} type={} channel={} attempt={}/{}",
                record.getId(), typeStr, channelStr, attempt, MAX_RETRIES);

        return templateService.resolve(record.getType(), record.getChannel(), "en", java.util.Map.of())
                .flatMap(template -> {
                    ChannelSender sender = senderRegistry.get(record.getChannel());
                    return sender.send(record.getRecipient(), template.getSubject(), template.getBody())
                            .then(Mono.defer(() -> {
                                record.setSendStatus("SENT");
                                record.setSentAt(Instant.now());
                                record.setRetryCount(attempt);
                                record.setErrorMessage(null);
                                return recordRepository.save(record).then();
                            }))
                            .onErrorResume(ex -> {
                                log.warn("Retry failed id={} attempt={}: {}", record.getId(), attempt, ex.getMessage());
                                record.setRetryCount(attempt);
                                record.setErrorMessage(truncate(ex.getMessage(), 500));
                                return recordRepository.save(record).then();
                            });
                })
                .onErrorResume(ex -> {
                    log.error("Template resolve failed for notification id={}: {}", record.getId(), ex.getMessage());
                    return Mono.empty();
                });
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
