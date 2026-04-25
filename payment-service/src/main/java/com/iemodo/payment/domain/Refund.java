package com.iemodo.payment.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Refund entity representing a refund transaction.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("refunds")
public class Refund extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("refund_no")
    private String refundNo;

    @Column("payment_id")
    private Long paymentId;

    @Column("order_id")
    private Long orderId;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("status")
    private RefundStatus refundStatus;

    @Column("reason_type")
    private RefundReason reasonType;

    @Column("reason_description")
    private String reasonDescription;

    @Column("third_party_refund_id")
    private String thirdPartyRefundId;

    @Column("third_party_refund_data")
    private Map<String, Object> thirdPartyRefundData;

    @Column("processed_at")
    private Instant processedAt;

    @Column("metadata")
    private Map<String, Object> metadata;

    @Column("tenant_id")
    private String tenantId;

    /**
     * Refund status
     */
    public enum RefundStatus {
        PENDING,     // Created, awaiting processing
        PROCESSING,  // Refund in progress
        SUCCESS,     // Refund completed
        FAILED       // Refund failed
    }

    /**
     * Refund reason types
     */
    public enum RefundReason {
        CUSTOMER_REQUEST,    // Customer requested refund
        DUPLICATE,           // Duplicate charge
        FRAUDULENT,          // Fraudulent transaction
        ORDER_CANCELLED,     // Order was cancelled
        PRODUCT_DEFECTIVE,   // Product was defective
        PRODUCT_NOT_RECEIVED, // Product not received
        WRONG_ITEM,          // Wrong item received
        OTHER                // Other reason
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
     * Mark as succeeded
     */
    public void markAsSucceeded(String thirdPartyRefundId) {
        this.refundStatus = RefundStatus.SUCCESS;
        this.thirdPartyRefundId = thirdPartyRefundId;
        this.processedAt = Instant.now();
    }

    /**
     * Mark as failed
     */
    public void markAsFailed(String reason) {
        this.refundStatus = RefundStatus.FAILED;
        this.reasonDescription = reason;
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        setIsValid(false);
    }

    /**
     * Check if refund is in final state
     */
    public boolean isFinalState() {
        return refundStatus == RefundStatus.SUCCESS || refundStatus == RefundStatus.FAILED;
    }
}
