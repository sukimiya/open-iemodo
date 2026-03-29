package com.iemodo.rma.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Line-item detail within an RMA request.
 * One row per SKU the customer wants to return/exchange.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("rma_items")
public class RmaItem extends BaseEntity {

    private Long rmaId;

    /** References order_items.id in order-service. */
    private Long orderItemId;

    private Long productId;
    private String sku;

    /** How many units of this SKU are being returned. */
    private Integer quantity;

    /** Unit price at time of original purchase (for refund calculation). */
    private BigDecimal unitPrice;

    /** Customer-stated reason for this specific item. */
    private String reason;

    /**
     * Physical condition of returned item (set by warehouse on receipt).
     * UNOPENED | USED | DAMAGED
     */
    private String condition;
}
