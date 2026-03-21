package com.iemodo.order.domain;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private Long id;

    private String orderNo;

    /** Tenant ID — denormalized for cross-schema audit queries. */
    private String tenantId;

    private Long customerId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private String currency;

    // ─── Shipping address (flattened into order row) ────────────────────────
    private String shippingName;
    private String shippingPhone;
    private String shippingAddr;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

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
        if (!this.status.canTransitionTo(next)) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_STATUS, HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Cannot transition from %s to %s", this.status, next));
        }
        this.status = next;
    }

    public boolean isPendingPayment() {
        return OrderStatus.PENDING_PAYMENT == status;
    }

    public boolean isCancellable() {
        return status.canTransitionTo(OrderStatus.CANCELLED);
    }
}
