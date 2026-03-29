package com.iemodo.notification.service;

import com.iemodo.notification.domain.NotificationChannel;
import com.iemodo.notification.domain.NotificationTemplate;
import com.iemodo.notification.domain.NotificationType;
import com.iemodo.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Resolves and renders notification templates.
 *
 * <p>Template resolution order:
 * <ol>
 *   <li>Exact match: type + channel + requested language</li>
 *   <li>Fallback: type + channel + "en"</li>
 *   <li>Hard-coded fallback string (so the pipeline never silently drops a notification)</li>
 * </ol>
 *
 * <p>Variable substitution uses simple {@code {{key}}} syntax — no external
 * template engine dependency needed for the current use cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    /**
     * Resolves the template for the given type/channel/language,
     * substitutes {@code variables}, and returns a rendered {@link NotificationTemplate}
     * (with subject and body filled in).
     */
    public Mono<NotificationTemplate> resolve(NotificationType type,
                                               NotificationChannel channel,
                                               String language,
                                               Map<String, String> variables) {
        return templateRepository
                .findByTypeAndChannelAndLanguageAndActiveTrue(type, channel, language)
                // Fallback to English — deferred so the second DB call only happens on cache miss
                .switchIfEmpty(Mono.defer(() ->
                        templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(type, channel, "en")))
                // Hard-coded fallback so we never drop a notification silently
                .switchIfEmpty(Mono.fromCallable(() -> buildFallback(type, channel)))
                .map(template -> render(template, variables))
                .doOnNext(t -> log.debug("Template resolved: type={} channel={} lang={}",
                        type, channel, t.getLanguage()));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /**
     * Substitutes all {@code {{key}}} occurrences in subject and body
     * with values from {@code variables}.
     */
    private NotificationTemplate render(NotificationTemplate template,
                                         Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) return template;

        String subject = template.getSubject();
        String body    = template.getBody();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            if (subject != null) subject = subject.replace(placeholder, value);
            if (body    != null) body    = body.replace(placeholder, value);
        }

        // Return a copy with rendered content — don't mutate the cached entity
        return NotificationTemplate.builder()
                .type(template.getType())
                .channel(template.getChannel())
                .language(template.getLanguage())
                .subject(subject)
                .body(body)
                .active(template.getActive())
                .build();
    }

    private NotificationTemplate buildFallback(NotificationType type, NotificationChannel channel) {
        log.warn("No template found for type={} channel={}, using fallback", type, channel);
        return NotificationTemplate.builder()
                .type(type)
                .channel(channel)
                .language("en")
                .subject("[iemodo] " + type.name().replace('_', ' ').toLowerCase())
                .body("You have a new notification from iemodo: " + type)
                .active(true)
                .build();
    }
}
