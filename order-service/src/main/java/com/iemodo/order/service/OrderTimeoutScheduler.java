package com.iemodo.order.service;

import com.iemodo.order.domain.DelayTaskStatus;
import com.iemodo.order.repository.OrderDelayTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Scans for unpaid orders whose payment window has expired and closes them.
 *
 * <p>Runs every 30 seconds. Multiple service instances are safe — the
 * {@code claimTask} query ensures each task is processed exactly once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderDelayTaskRepository orderDelayTaskRepository;
    private final OrderService             orderService;

    @Scheduled(fixedDelay = 30_000)
    public void processTimeoutOrders() {
        orderDelayTaskRepository
                .findByExecuteTimeBeforeAndTaskStatus(Instant.now(), DelayTaskStatus.PENDING)
                .flatMap(task -> orderService.cancelTimedOutOrder(task)
                        .onErrorResume(ex -> {
                            log.error("Failed to process timeout task={} order={}",
                                    task.getId(), task.getOrderId(), ex);
                            return Mono.empty();
                        }))
                .subscribe(
                        null,
                        ex -> log.error("Timeout scheduler fatal error", ex)
                );
    }
}
