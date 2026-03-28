package com.iemodo.inventory.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Inventory transaction entity - records all stock movements.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("inventory_transactions")
public class InventoryTransaction extends BaseEntity {
    // id is inherited from BaseEntity

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
