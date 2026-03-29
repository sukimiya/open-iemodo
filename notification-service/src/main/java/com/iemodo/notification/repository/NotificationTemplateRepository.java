package com.iemodo.notification.repository;

import com.iemodo.notification.domain.NotificationChannel;
import com.iemodo.notification.domain.NotificationTemplate;
import com.iemodo.notification.domain.NotificationType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface NotificationTemplateRepository
        extends ReactiveCrudRepository<NotificationTemplate, Long> {

    Mono<NotificationTemplate> findByTypeAndChannelAndLanguageAndActiveTrue(
            NotificationType type, NotificationChannel channel, String language);

    /** Fallback to English when the requested language template doesn't exist. */
    Mono<NotificationTemplate> findByTypeAndChannelAndLanguageAndActiveTrueOrderByLanguageAsc(
            NotificationType type, NotificationChannel channel, String language);
}
