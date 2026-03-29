package com.iemodo.notification.repository;

import com.iemodo.notification.domain.NotificationRecord;
import com.iemodo.notification.domain.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRecordRepository
        extends ReactiveCrudRepository<NotificationRecord, Long> {

    Flux<NotificationRecord> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Flux<NotificationRecord> findBySendStatusOrderByCreateTimeAsc(String sendStatus, Pageable pageable);

    /** Idempotency check — has this exact event already been sent? */
    Mono<Boolean> existsByUserIdAndTypeAndSendStatus(Long userId, NotificationType type, String sendStatus);
}
