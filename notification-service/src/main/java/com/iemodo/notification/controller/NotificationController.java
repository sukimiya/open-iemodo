package com.iemodo.notification.controller;

import com.iemodo.notification.dto.NotificationDTO;
import com.iemodo.notification.dto.SendNotificationRequest;
import com.iemodo.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST entry-point for the notification service.
 *
 * <p>Typical callers: order-service, rma-service, review-service — all internal.
 * The API gateway should restrict {@code /notify/**} to service-to-service traffic only.
 */
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Trigger a single notification.
     *
     * <p>Returns 202 Accepted — the record is persisted synchronously but the
     * actual delivery (especially email) may be async on a bounded-elastic thread.
     */
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<NotificationDTO> send(@Valid @RequestBody SendNotificationRequest request) {
        return notificationService.send(request);
    }

    /**
     * Fetch notification history for a user (newest first).
     */
    @GetMapping("/user/{userId}")
    public Flux<NotificationDTO> listByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.listByUser(userId, page, size);
    }

    /**
     * Internal endpoint: list FAILED records for retry (consumed by a scheduled job).
     */
    @GetMapping("/failed")
    public Flux<NotificationDTO> listFailed(
            @RequestParam(defaultValue = "50") int limit) {
        return notificationService.listFailed(limit);
    }
}
