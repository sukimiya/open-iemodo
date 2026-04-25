package com.iemodo.notification.repository;

import com.iemodo.notification.domain.NotificationRecord;
import com.iemodo.notification.domain.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRecordRepository
        extends ReactiveCrudRepository<NotificationRecord, Long> {

    Flux<NotificationRecord> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Flux<NotificationRecord> findBySendStatusOrderByCreateTimeAsc(String sendStatus, Pageable pageable);

    /** Idempotency check — has this exact event already been sent? */
    Mono<Boolean> existsByUserIdAndTypeAndSendStatus(Long userId, NotificationType type, String sendStatus);

    /** Failed notifications eligible for retry, ordered oldest-first. */
    @Query("SELECT * FROM notification_records WHERE send_status = 'FAILED' AND retry_count < :maxRetries AND is_valid = true ORDER BY create_time ASC LIMIT :limit")
    Flux<NotificationRecord> findFailedForRetry(int maxRetries, int limit);
}
