package com.iemodo.order.domain;

import java.util.Set;

/**
 * Order lifecycle state machine.
 *
 * <pre>
 * PENDING_PAYMENT ──► PAID ──► PROCESSING ──► SHIPPED ──► DELIVERED
 *       │                                                      │
 *       └──► CANCELLED                       PAID/PROCESSING ─┘
 *                                                 └──► REFUNDED
 * </pre>
 */
public enum OrderStatus {

    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    /**
     * Returns {@code true} if transitioning from {@code this} to {@code next}
     * is a legal state-machine move.
     */
    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }

    private Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(PAID, CANCELLED);
            case PAID            -> Set.of(PROCESSING, REFUNDED);
            case PROCESSING      -> Set.of(SHIPPED, REFUNDED);
            case SHIPPED         -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED, REFUNDED -> Set.of(); // terminal states
        };
    }
}
