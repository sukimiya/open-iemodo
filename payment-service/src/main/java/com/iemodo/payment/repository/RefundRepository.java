package com.iemodo.payment.repository;

import com.iemodo.payment.domain.Refund;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Refund repository
 */
@Repository
public interface RefundRepository extends ReactiveCrudRepository<Refund, Long> {

    Mono<Refund> findByRefundNo(String refundNo);

    Mono<Refund> findByRefundNoAndIsValid(String refundNo);

    Mono<Refund> findByIdAndIsValid(Long id);

    Flux<Refund> findByPaymentIdAndIsValidOrderByCreateTimeDesc(Long paymentId);

    Flux<Refund> findByOrderIdAndIsValidOrderByCreateTimeDesc(Long orderId);

    @Query("SELECT * FROM refunds WHERE tenant_id = :tenantId AND is_valid = 1 ORDER BY create_time DESC LIMIT :limit OFFSET :offset")
    Flux<Refund> findByTenantId(String tenantId, int limit, int offset);

    @Query("SELECT COUNT(*) FROM refunds WHERE tenant_id = :tenantId AND is_valid = 1")
    Mono<Long> countByTenantId(String tenantId);

    Mono<Boolean> existsByRefundNo(String refundNo);

    @Query("SELECT * FROM refunds WHERE third_party_refund_id = :refundId AND is_valid = 1")
    Mono<Refund> findByThirdPartyRefundId(String refundId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM refunds WHERE payment_id = :paymentId AND status = 'SUCCESS' AND is_valid = 1")
    Mono<java.math.BigDecimal> sumSuccessfulRefundsByPaymentId(Long paymentId);

    @Query("UPDATE refunds SET status = :status, third_party_refund_id = :thirdPartyId, processed_at = NOW(), update_time = NOW() WHERE id = :id")
    Mono<Integer> markAsSucceeded(Long id, String status, String thirdPartyId);

    @Query("UPDATE refunds SET status = 'FAILED', reason_description = :reason, update_time = NOW() WHERE id = :id")
    Mono<Integer> markAsFailed(Long id, String reason);
}
