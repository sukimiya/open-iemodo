package com.iemodo.payment.domain;

import com.iemodo.common.entity.BaseEntity;
import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Payment entity representing a payment transaction.
 * PCI DSS compliant - does not store full card numbers or CVV.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("payments")
public class Payment extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("payment_no")
    private String paymentNo;

    @Column("order_id")
    private Long orderId;

    @Column("order_no")
    private String orderNo;

    @Column("customer_id")
    private Long customerId;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("channel")
    private PaymentChannel channel;

    @Column("channel_sub_type")
    private String channelSubType;

    @Column("payment_method_id")
    private String paymentMethodId;

    @Column("payment_method_type")
    private String paymentMethodType;

    @Column("payment_method_last4")
    private String paymentMethodLast4;

    @Column("payment_method_brand")
    private String paymentMethodBrand;

    @Column("status")
    private PaymentStatus paymentStatus;

    @Column("third_party_txn_id")
    private String thirdPartyTxnId;

    @Column("third_party_txn_data")
    private Map<String, Object> thirdPartyTxnData;

    @Column("paid_at")
    private Instant paidAt;

    @Column("expired_at")
    private Instant expiredAt;

    @Column("refunded_amount")
    private BigDecimal refundedAmount;

    @Transient
    private BigDecimal refundableAmount;

    @Column("failure_code")
    private String failureCode;

    @Column("failure_message")
    private String failureMessage;

    @Column("description")
    private String description;

    @Column("metadata")
    private Map<String, Object> metadata;

    @Column("tenant_id")
    private String tenantId;

    /**
     * Payment channels
     */
    public enum PaymentChannel {
        STRIPE, PAYPAL, ALIPAY, WECHAT_PAY
    }

    /**
     * Payment status with formal state machine transitions.
     *
     * <pre>
     * PENDING ──► PROCESSING ──► SUCCESS ──► REFUNDED
     *    │              │            │
     *    └──► FAILED    └──► FAILED  └──► PARTIALLY_REFUNDED ──► REFUNDED
     *    └──► CANCELLED └──► CANCELLED
     * </pre>
     */
    public enum PaymentStatus {
        PENDING,             // Created, awaiting payment
        PROCESSING,          // Payment in progress
        SUCCESS,             // Payment completed
        FAILED,              // Payment failed
        CANCELLED,           // Cancelled by user or system
        REFUNDED,            // Fully refunded
        PARTIALLY_REFUNDED;  // Partially refunded

        public boolean canTransitionTo(PaymentStatus next) {
            return allowedTransitions().contains(next);
        }

        private Set<PaymentStatus> allowedTransitions() {
            return switch (this) {
                case PENDING             -> Set.of(PROCESSING, SUCCESS, FAILED, CANCELLED);
                case PROCESSING          -> Set.of(SUCCESS, FAILED, CANCELLED);
                case SUCCESS             -> Set.of(REFUNDED, PARTIALLY_REFUNDED);
                case PARTIALLY_REFUNDED  -> Set.of(REFUNDED, PARTIALLY_REFUNDED);
                case FAILED, CANCELLED, REFUNDED -> Set.of();
            };
        }
    }

    /**
     * Compatibility method for createdAt
     */
    public Instant getCreatedAt() {
        return getCreateTime();
    }

    /**
     * Compatibility method for updatedAt
     */
    public Instant getUpdatedAt() {
        return getUpdateTime();
    }

    /**
     * Check if payment can be refunded
     */
    public boolean canRefund() {
        return paymentStatus == PaymentStatus.SUCCESS || 
               paymentStatus == PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * Check if payment is in final state
     */
    public boolean isFinalState() {
        return paymentStatus == PaymentStatus.SUCCESS ||
               paymentStatus == PaymentStatus.FAILED ||
               paymentStatus == PaymentStatus.CANCELLED ||
               paymentStatus == PaymentStatus.REFUNDED;
    }

    /**
     * Check if payment is pending
     */
    public boolean isPending() {
        return paymentStatus == PaymentStatus.PENDING;
    }

    /**
     * Get refundable amount
     */
    public BigDecimal getRefundableAmount() {
        if (refundedAmount == null || amount == null) {
            return amount != null ? amount : BigDecimal.ZERO;
        }
        return amount.subtract(refundedAmount);
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        setIsValid(0);
    }

    /**
     * Check if expired
     */
    public boolean isExpired() {
        return expiredAt != null && Instant.now().isAfter(expiredAt);
    }

    /**
     * Validated status transition — throws if the move is not allowed by the state machine.
     */
    public void transitionTo(PaymentStatus next) {
        if (!this.paymentStatus.canTransitionTo(next)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PAYMENT_STATUS, HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Cannot transition payment from %s to %s", paymentStatus, next));
        }
        this.paymentStatus = next;
    }

    /**
     * Mark as paid
     */
    public void markAsPaid(String thirdPartyTxnId) {
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.thirdPartyTxnId = thirdPartyTxnId;
        this.paidAt = Instant.now();
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(String code, String message) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failureCode = code;
        this.failureMessage = message;
    }

    /**
     * Add refund amount
     */
    public void addRefundAmount(BigDecimal refundAmount) {
        if (this.refundedAmount == null) {
            this.refundedAmount = BigDecimal.ZERO;
        }
        this.refundedAmount = this.refundedAmount.add(refundAmount);
        
        // Update status based on refund amount
        if (this.refundedAmount.compareTo(this.amount) >= 0) {
            this.paymentStatus = PaymentStatus.REFUNDED;
        } else if (this.refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }
}
