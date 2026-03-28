package com.iemodo.inventory.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stock transfer entity - represents a movement of stock between warehouses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("stock_transfers")
public class StockTransfer {

    @Id
    private Long id;

    private String transferNo;

    private Long fromWarehouseId;
    private Long toWarehouseId;

    /** Status: PENDING, APPROVED, SHIPPED, RECEIVED, CANCELLED */
    private String status;

    // Costs
    private BigDecimal shippingCost;
    private BigDecimal handlingCost;
    private BigDecimal totalCost;

    // Tracking
    private String carrier;
    private String trackingNo;

    // Timestamps
    private Instant approvedAt;
    private Instant shippedAt;
    private Instant receivedAt;

    private String notes;

    private Long createdBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public enum Status {
        PENDING, APPROVED, SHIPPED, RECEIVED, CANCELLED
    }

    public boolean canApprove() {
        return Status.PENDING.name().equals(status);
    }

    public boolean canShip() {
        return Status.APPROVED.name().equals(status);
    }

    public boolean canReceive() {
        return Status.SHIPPED.name().equals(status);
    }

    public boolean canCancel() {
        return Status.PENDING.name().equals(status) || 
               Status.APPROVED.name().equals(status);
    }

    public void approve() {
        this.status = Status.APPROVED.name();
        this.approvedAt = Instant.now();
    }

    public void ship() {
        this.status = Status.SHIPPED.name();
        this.shippedAt = Instant.now();
    }

    public void receive() {
        this.status = Status.RECEIVED.name();
        this.receivedAt = Instant.now();
    }

    public void cancel() {
        this.status = Status.CANCELLED.name();
    }
}
