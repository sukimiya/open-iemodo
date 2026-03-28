package com.iemodo.inventory.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Inventory transaction entity - records all stock movements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("inventory_transactions")
public class InventoryTransaction {

    @Id
    private Long id;

    private Long warehouseId;
    private Long skuId;

    /** Transaction type: INBOUND, OUTBOUND, ADJUST, TRANSFER_IN, TRANSFER_OUT */
    private String transactionType;

    /** Quantity change (positive for inbound, negative for outbound) */
    private Integer quantity;

    // Before/After quantities for audit
    private Integer beforeAvailable;
    private Integer afterAvailable;
    private Integer beforeReserved;
    private Integer afterReserved;

    // Reference
    private String referenceNo;      // Order No, Transfer No
    private String referenceType;    // ORDER, TRANSFER, ADJUSTMENT

    // Details
    private String reason;
    private String notes;

    private Long createdBy;

    @CreatedDate
    private Instant createdAt;

    // ─── Transaction types ─────────────────────────────────────────────────

    public enum TransactionType {
        INBOUND,        // Stock received
        OUTBOUND,       // Stock shipped
        ADJUST,         // Manual adjustment
        TRANSFER_IN,    // Transfer received
        TRANSFER_OUT,   // Transfer shipped
        RESERVED,       // Stock reserved
        RELEASED        // Reservation released
    }

    public enum ReferenceType {
        ORDER, TRANSFER, ADJUSTMENT, RETURN, SYSTEM
    }
}
