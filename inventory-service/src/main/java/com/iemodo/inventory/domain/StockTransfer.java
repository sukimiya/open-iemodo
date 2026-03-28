package com.iemodo.inventory.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stock transfer entity - represents a movement of stock between warehouses.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("stock_transfers")
public class StockTransfer extends BaseEntity {
    // id is inherited from BaseEntity

    private String transferNo;

    private Long fromWarehouseId;
    private Long toWarehouseId;

    /** Status: PENDING, APPROVED, SHIPPED, RECEIVED, CANCELLED */
    private String transferStatus;

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

    // ─── Domain helpers ────────────────────────────────────────────────────

    public enum Status {
        PENDING, APPROVED, SHIPPED, RECEIVED, CANCELLED
    }

    public boolean canApprove() {
        return Status.PENDING.name().equals(transferStatus);
    }

    public boolean canShip() {
        return Status.APPROVED.name().equals(transferStatus);
    }

    public boolean canReceive() {
        return Status.SHIPPED.name().equals(transferStatus);
    }

    public boolean canCancel() {
        return Status.PENDING.name().equals(transferStatus) || 
               Status.APPROVED.name().equals(transferStatus);
    }

    public void approve() {
        this.transferStatus = Status.APPROVED.name();
        this.approvedAt = Instant.now();
    }

    public void ship() {
        this.transferStatus = Status.SHIPPED.name();
        this.shippedAt = Instant.now();
    }

    public void receive() {
        this.transferStatus = Status.RECEIVED.name();
        this.receivedAt = Instant.now();
    }

    public void cancel() {
        this.transferStatus = Status.CANCELLED.name();
    }
}
