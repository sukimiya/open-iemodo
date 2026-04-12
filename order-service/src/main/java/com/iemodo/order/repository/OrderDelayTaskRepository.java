package com.iemodo.order.repository;

import com.iemodo.order.domain.DelayTaskStatus;
import com.iemodo.order.domain.OrderDelayTask;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface OrderDelayTaskRepository extends R2dbcRepository<OrderDelayTask, Long> {

    Flux<OrderDelayTask> findByExecuteTimeBeforeAndTaskStatus(Instant executeTime, DelayTaskStatus taskStatus);

    /**
     * Atomically claims a task by flipping its status from PENDING to PROCESSING.
     * Returns the number of rows updated (1 = claimed, 0 = already taken by another instance).
     */
    @Modifying
    @Query("UPDATE order_delay_task SET task_status = 'PROCESSING' WHERE id = :id AND task_status = 'PENDING'")
    Mono<Integer> claimTask(Long id);

    @Modifying
    @Query("UPDATE order_delay_task SET task_status = :taskStatus WHERE id = :id")
    Mono<Integer> updateStatus(Long id, String taskStatus);
}
