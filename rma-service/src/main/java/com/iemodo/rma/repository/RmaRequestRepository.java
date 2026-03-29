package com.iemodo.rma.repository;

import com.iemodo.rma.domain.RmaRequest;
import com.iemodo.rma.domain.RmaStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RmaRequestRepository extends ReactiveCrudRepository<RmaRequest, Long> {

    Mono<RmaRequest> findByRmaNo(String rmaNo);

    Flux<RmaRequest> findByCustomerIdOrderByCreateTimeDesc(Long customerId, Pageable pageable);

    Flux<RmaRequest> findByTenantIdAndRmaStatusOrderByCreateTimeDesc(
            String tenantId, RmaStatus status, Pageable pageable);

    Flux<RmaRequest> findByOrderId(Long orderId);

    /** Optimistic-lock update — returns 0 if version mismatch (concurrent modification). */
    @org.springframework.data.r2dbc.repository.Query("""
        UPDATE rma_requests
           SET rma_status = :newStatus,
               last_operator_id = :operatorId,
               update_time = NOW()
         WHERE id = :id
           AND rma_status = :expectedStatus
        """)
    Mono<Integer> compareAndSetStatus(Long id, RmaStatus expectedStatus,
                                      RmaStatus newStatus, Long operatorId);
}
