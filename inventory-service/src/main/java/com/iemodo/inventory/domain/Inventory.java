package com.iemodo.inventory.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Inventory entity - represents stock levels for an SKU in a warehouse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("inventory")
public class Inventory {

    @Id
    private Long id;

    private Long warehouseId;
    private Long skuId;

    // Stock quantities
    private Integer availableQty;
    private Integer reservedQty;
    private Integer lockedQty;
    private Integer inboundQty;

    // Computed (read-only)
    private Integer sellableQty;
    private Integer totalQty;

    // Thresholds
    private Integer minStockQty;
    private Integer maxStockQty;
    private Integer reorderPoint;

    // Optimistic locking
    @Version
    private Integer version;

    // Tracking
    private Instant lastStockInAt;
    private Instant lastStockOutAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Domain helpers ────────────────────────────────────────────────────

    public int getAvailable() {
        return availableQty != null ? availableQty : 0;
    }

    public int getReserved() {
        return reservedQty != null ? reservedQty : 0;
    }

    public int getLocked() {
        return lockedQty != null ? lockedQty : 0;
    }

    public int getInbound() {
        return inboundQty != null ? inboundQty : 0;
    }

    public int getSellable() {
        return getAvailable() - getReserved();
    }

    public int getTotal() {
        return getAvailable() + getReserved() + getLocked() + getInbound();
    }

    public boolean hasEnoughStock(int quantity) {
        return getSellable() >= quantity;
    }

    public boolean isLowStock() {
        if (reorderPoint == null) return false;
        return getAvailable() <= reorderPoint;
    }

    public boolean isOverstock() {
        if (maxStockQty == null) return false;
        return getTotal() > maxStockQty;
    }

    // ─── Stock operations ──────────────────────────────────────────────────

    public void reserve(int quantity) {
        this.reservedQty = getReserved() + quantity;
    }

    public void release(int quantity) {
        this.reservedQty = Math.max(0, getReserved() - quantity);
    }

    public void confirm(int quantity) {
        this.availableQty = Math.max(0, getAvailable() - quantity);
        this.reservedQty = Math.max(0, getReserved() - quantity);
        this.lastStockOutAt = Instant.now();
    }

    public void inbound(int quantity) {
        this.availableQty = getAvailable() + quantity;
        this.lastStockInAt = Instant.now();
    }

    public void lock(int quantity) {
        int toLock = Math.min(getAvailable(), quantity);
        this.availableQty = getAvailable() - toLock;
        this.lockedQty = getLocked() + toLock;
    }

    public void unlock(int quantity) {
        int toUnlock = Math.min(getLocked(), quantity);
        this.lockedQty = getLocked() - toUnlock;
        this.availableQty = getAvailable() + toUnlock;
    }

    public void adjust(int delta) {
        this.availableQty = Math.max(0, getAvailable() + delta);
    }
}
