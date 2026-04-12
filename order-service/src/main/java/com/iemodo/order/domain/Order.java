package com.iemodo.order.domain;

import com.iemodo.common.entity.BaseEntity;
import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Order aggregate root.
 *
 * <p>Maps to the {@code orders} table in the current tenant's schema.
 * Order items are stored in {@code order_items} and loaded separately
 * (R2DBC does not support nested collections natively).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("orders")
public class Order extends BaseEntity {

    // id is inherited from BaseEntity

    @Version
    private Integer version;

    private String orderNo;

    /** Tenant ID — denormalized for cross-schema audit queries. */
    private String tenantId;

    private Long customerId;

    /** Order status (PENDING_PAYMENT, PAID, SHIPPED, etc.) - renamed to avoid conflict with BaseEntity.status */
    private OrderStatus orderStatus;

    private BigDecimal totalAmount;

    private String currency;

    // ─── Shipping address (flattened into order row) ────────────────────────
    private String shippingName;
    private String shippingPhone;
    private String shippingAddr;

    /** Loaded separately after the order row is fetched — not persisted here. */
    @Transient
    private List<OrderItem> items = new ArrayList<>();

    // ─── State machine ──────────────────────────────────────────────────────

    /**
     * Transition the order to {@code next} status.
     *
     * @throws BusinessException if the transition is not allowed
     */
    public void transitionTo(OrderStatus next) {
        if (!this.orderStatus.canTransitionTo(next)) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_STATUS, HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Cannot transition from %s to %s", this.orderStatus, next));
        }
        this.orderStatus = next;
    }

    public boolean isPendingPayment() {
        return OrderStatus.PENDING_PAYMENT == orderStatus;
    }

    public boolean isCancellable() {
        return orderStatus.canTransitionTo(OrderStatus.CANCELLED);
    }

    // ─── Compatibility methods for BaseEntity audit fields ─────────────────

    public Instant getCreatedAt() {
        return getCreateTime();
    }

    public Instant getUpdatedAt() {
        return getUpdateTime();
    }
}
