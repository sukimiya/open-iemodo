package com.iemodo.notification.service;

import com.iemodo.notification.config.ChannelSenderRegistry;
import com.iemodo.notification.domain.NotificationRecord;
import com.iemodo.notification.dto.NotificationDTO;
import com.iemodo.notification.dto.SendNotificationRequest;
import com.iemodo.notification.repository.NotificationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Central notification dispatcher.
 *
 * <p>Flow per request:
 * <ol>
 *   <li>Persist a PENDING record (gives us an audit trail even if send fails)</li>
 *   <li>Resolve + render template via {@link TemplateService}</li>
 *   <li>Delegate to the appropriate {@link com.iemodo.notification.channel.ChannelSender}</li>
 *   <li>Update record to SENT (or FAILED + errorMessage)</li>
 * </ol>
 *
 * <p>Retry and dead-letter handling are intentionally left to the caller / a future
 * scheduled job — this class concerns itself only with a single send attempt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TemplateService templateService;
    private final ChannelSenderRegistry senderRegistry;
    private final NotificationRecordRepository recordRepository;

    // ─── Send ─────────────────────────────────────────────────────────────

    public Mono<NotificationDTO> send(SendNotificationRequest request) {
        String lang = request.getLanguage() != null ? request.getLanguage() : "zh-CN";

        // 1. Create PENDING record up-front so we have an audit trail
        NotificationRecord pending = NotificationRecord.builder()
                .tenantId(request.getTenantId())
                .userId(request.getUserId())
                .channel(request.getChannel())
                .type(request.getType())
                .recipient(request.getRecipient())
                .sendStatus("PENDING")
                .retryCount(0)
                .build();

        return recordRepository.save(pending)
                .flatMap(saved -> templateService
                        .resolve(request.getType(), request.getChannel(), lang, request.getVariables())
                        .flatMap(template -> {
                            // 2. Attempt delivery
                            var sender = senderRegistry.get(request.getChannel());
                            return sender.send(request.getRecipient(), template.getSubject(), template.getBody())
                                    // 3a. On success — update record to SENT
                                    .then(Mono.defer(() -> {
                                        saved.setSendStatus("SENT");
                                        saved.setSentAt(Instant.now());
                                        saved.setSubject(template.getSubject());
                                        saved.setBody(template.getBody());
                                        return recordRepository.save(saved);
                                    }))
                                    // 3b. On error — update record to FAILED, do NOT propagate
                                    .onErrorResume(ex -> {
                                        log.error("Notification send failed: type={} channel={} recipient={}",
                                                request.getType(), request.getChannel(), request.getRecipient(), ex);
                                        saved.setSendStatus("FAILED");
                                        saved.setRetryCount(saved.getRetryCount() + 1);
                                        saved.setErrorMessage(truncate(ex.getMessage(), 500));
                                        saved.setSubject(template.getSubject());
                                        saved.setBody(template.getBody());
                                        return recordRepository.save(saved);
                                    });
                        }))
                .map(this::toDTO);
    }

    // ─── Queries ──────────────────────────────────────────────────────────

    public Flux<NotificationDTO> listByUser(Long userId, int page, int size) {
        return recordRepository
                .findByUserIdOrderByCreateTimeDesc(userId, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    /**
     * Returns up to {@code limit} FAILED records for retry by a scheduled job.
     */
    public Flux<NotificationDTO> listFailed(int limit) {
        return recordRepository
                .findBySendStatusOrderByCreateTimeAsc("FAILED", PageRequest.of(0, limit))
                .map(this::toDTO);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private NotificationDTO toDTO(NotificationRecord r) {
        return NotificationDTO.builder()
                .id(r.getId())
                .tenantId(r.getTenantId())
                .userId(r.getUserId())
                .channel(r.getChannel())
                .type(r.getType())
                .recipient(r.getRecipient())
                .subject(r.getSubject())
                .sendStatus(r.getSendStatus())
                .retryCount(r.getRetryCount())
                .sentAt(r.getSentAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
