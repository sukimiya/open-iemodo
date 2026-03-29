package com.iemodo.rma.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * RMA aggregate root.
 *
 * <p>The {@code regionSnapshot} field is a JSON string capturing the
 * {@link RmaRegionConfig} at creation time, so that subsequent config
 * changes cannot alter in-flight requests.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("rma_requests")
public class RmaRequest extends BaseEntity {

    private String rmaNo;
    private String tenantId;
    private Long orderId;
    private Long customerId;

    private RmaType type;
    private RmaStatus rmaStatus;

    // ─── Region policy ─────────────────────────────────────────────────────

    /** Logical region code applied at creation time. */
    private String regionCode;

    /**
     * JSON snapshot of {@link RmaRegionConfig} captured at creation.
     * Stored as TEXT in PostgreSQL — deserialized in the service layer.
     */
    private String regionSnapshot;

    // ─── Customer input ────────────────────────────────────────────────────

    private String reason;
    private String description;

    // ─── Refund details (populated at APPROVED time) ───────────────────────

    private BigDecimal refundAmount;
    private String refundCurrency;

    /** Whether import tax is included in refundAmount. */
    private Boolean taxRefundIncluded;

    // ─── Return logistics (populated when buyer submits tracking) ──────────

    private String trackingNo;
    private String carrier;

    // ─── Operator notes ────────────────────────────────────────────────────

    private String merchantNotes;

    /** ID of the last operator who changed the status. */
    private Long lastOperatorId;

    // ─── Timestamps ────────────────────────────────────────────────────────

    private Instant approvedAt;
    private Instant receivedAt;
    private Instant completedAt;

    // ─── Items (loaded separately, not persisted here) ─────────────────────

    @Transient
    private List<RmaItem> items = new ArrayList<>();

    // ─── State machine ─────────────────────────────────────────────────────

    /**
     * Attempt to transition to {@code next}.
     * Delegates to {@link RmaStatus#canTransitionTo} for type-aware validation.
     *
     * @throws IllegalStateException if the transition is not allowed
     */
    public void transitionTo(RmaStatus next, Long operatorId) {
        if (rmaStatus.isTerminal()) {
            throw new IllegalStateException(
                "RMA " + rmaNo + " is already in terminal status " + rmaStatus);
        }
        if (!rmaStatus.canTransitionTo(next, type)) {
            throw new IllegalStateException(String.format(
                "RMA %s [type=%s]: cannot transition %s → %s", rmaNo, type, rmaStatus, next));
        }
        this.rmaStatus = next;
        this.lastOperatorId = operatorId;

        // Set convenience timestamps
        switch (next) {
            case APPROVED   -> this.approvedAt = Instant.now();
            case RECEIVED   -> this.receivedAt = Instant.now();
            case COMPLETED  -> this.completedAt = Instant.now();
            default -> { /* no extra timestamp needed */ }
        }
    }

    public Instant getCreatedAt() { return getCreateTime(); }
    public Instant getUpdatedAt() { return getUpdateTime(); }
}
