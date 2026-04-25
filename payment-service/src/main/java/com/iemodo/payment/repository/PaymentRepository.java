package com.iemodo.payment.repository;

import com.iemodo.payment.domain.Payment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Payment repository
 */
@Repository
public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {

    Mono<Payment> findByPaymentNo(String paymentNo);

    @Query("SELECT * FROM payments WHERE payment_no = :paymentNo AND is_valid = true")
    Mono<Payment> findByPaymentNoAndIsValid(String paymentNo);

    @Query("SELECT * FROM payments WHERE id = :id AND is_valid = true")
    Mono<Payment> findByIdAndIsValid(Long id);

    @Query("SELECT * FROM payments WHERE order_id = :orderId AND is_valid = true")
    Flux<Payment> findByOrderIdAndIsValid(Long orderId);

    @Query("SELECT * FROM payments WHERE order_no = :orderNo AND is_valid = true")
    Flux<Payment> findByOrderNoAndIsValid(String orderNo);

    @Query("SELECT * FROM payments WHERE customer_id = :customerId AND is_valid = true ORDER BY create_time DESC")
    Flux<Payment> findByCustomerIdAndIsValidOrderByCreateTimeDesc(Long customerId);

    @Query("SELECT * FROM payments WHERE payment_status = :paymentStatus AND is_valid = true")
    Flux<Payment> findByPaymentStatusAndIsValid(Payment.PaymentStatus paymentStatus);

    @Query("SELECT * FROM payments WHERE payment_status = :paymentStatus AND expired_at < :now AND is_valid = true")
    Flux<Payment> findByPaymentStatusAndExpiredAtBeforeAndIsValid(Payment.PaymentStatus paymentStatus, Instant now);

    @Query("SELECT * FROM payments WHERE tenant_id = :tenantId AND is_valid = true ORDER BY create_time DESC LIMIT :limit OFFSET :offset")
    Flux<Payment> findByTenantId(String tenantId, int limit, int offset);

    @Query("SELECT COUNT(*) FROM payments WHERE tenant_id = :tenantId AND is_valid = true")
    Mono<Long> countByTenantId(String tenantId);

    Mono<Boolean> existsByPaymentNo(String paymentNo);

    @Query("SELECT * FROM payments WHERE third_party_txn_id = :txnId AND is_valid = true")
    Mono<Payment> findByThirdPartyTxnId(String txnId);

    @Query("UPDATE payments SET status = :status, update_time = NOW() WHERE id = :id")
    Mono<Integer> updateStatus(Long id, String status);

    @Query("UPDATE payments SET status = :status, third_party_txn_id = :txnId, paid_at = NOW(), update_time = NOW() WHERE id = :id")
    Mono<Integer> markAsPaid(Long id, String status, String txnId);

    @Query("UPDATE payments SET status = 'FAILED', failure_code = :code, failure_message = :message, update_time = NOW() WHERE id = :id")
    Mono<Integer> markAsFailed(Long id, String code, String message);

    @Query("UPDATE payments SET refunded_amount = refunded_amount + :amount, status = :newStatus, update_time = NOW() WHERE id = :id")
    Mono<Integer> addRefundAmount(Long id, java.math.BigDecimal amount, String newStatus);

    /**
     * Find payments stuck in PENDING or PROCESSING with a known third-party transaction ID,
     * created before {@code cutoff}. Used by the reconciliation job to detect lost webhooks.
     */
    @Query("SELECT * FROM payments WHERE payment_status IN ('PENDING', 'PROCESSING') AND create_time < :cutoff AND third_party_txn_id IS NOT NULL AND is_valid = true")
    Flux<Payment> findStuckPayments(Instant cutoff);
}
