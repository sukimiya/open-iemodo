package com.iemodo.rma.domain;

import java.util.Map;
import java.util.Set;

/**
 * RMA lifecycle state machine — one shared enum for all {@link RmaType}s.
 *
 * <p>Allowed transitions per type:
 * <pre>
 * RETURN / EXCHANGE:
 *   PENDING_REVIEW → APPROVED → WAITING_SHIPMENT → IN_TRANSIT → RECEIVED
 *                                                                    │
 *                 RETURN ────────────────────────────────────────► REFUNDING → COMPLETED
 *                 EXCHANGE ──────────────────────────────────────► INSPECTING → RESHIPPING → COMPLETED
 *                                                                            └──► REFUNDING → COMPLETED
 *
 * REFUND_ONLY:
 *   PENDING_REVIEW → APPROVED → REFUNDING → COMPLETED
 *
 * Any status → REJECTED (terminal) or CANCELLED (terminal) from early states.
 * </pre>
 */
public enum RmaStatus {

    PENDING_REVIEW,
    APPROVED,
    REJECTED,           // terminal — merchant denied the request
    WAITING_SHIPMENT,   // buyer must now send goods back
    IN_TRANSIT,         // buyer shipped goods, tracking provided
    RECEIVED,           // warehouse confirmed receipt
    INSPECTING,         // quality check in progress (EXCHANGE only)
    REFUNDING,          // refund being processed by payment-service
    RESHIPPING,         // replacement being sent (EXCHANGE only)
    COMPLETED,          // terminal — fully done
    CANCELLED,          // terminal — cancelled before approval
    FAILED;             // terminal — e.g. refund failed after retries

    // ─── Transition tables (one per RmaType) ───────────────────────────────

    private static final Map<RmaStatus, Set<RmaStatus>> RETURN_TRANSITIONS = Map.of(
        PENDING_REVIEW,   Set.of(APPROVED, REJECTED, CANCELLED),
        APPROVED,         Set.of(WAITING_SHIPMENT, CANCELLED),
        WAITING_SHIPMENT, Set.of(IN_TRANSIT, CANCELLED),
        IN_TRANSIT,       Set.of(RECEIVED),
        RECEIVED,         Set.of(REFUNDING),
        REFUNDING,        Set.of(COMPLETED, FAILED)
    );

    private static final Map<RmaStatus, Set<RmaStatus>> EXCHANGE_TRANSITIONS = Map.of(
        PENDING_REVIEW,   Set.of(APPROVED, REJECTED, CANCELLED),
        APPROVED,         Set.of(WAITING_SHIPMENT, CANCELLED),
        WAITING_SHIPMENT, Set.of(IN_TRANSIT, CANCELLED),
        IN_TRANSIT,       Set.of(RECEIVED),
        RECEIVED,         Set.of(INSPECTING),
        INSPECTING,       Set.of(RESHIPPING, REFUNDING),  // inspection fail → refund instead
        RESHIPPING,       Set.of(COMPLETED),
        REFUNDING,        Set.of(COMPLETED, FAILED)
    );

    private static final Map<RmaStatus, Set<RmaStatus>> REFUND_ONLY_TRANSITIONS = Map.of(
        PENDING_REVIEW, Set.of(APPROVED, REJECTED, CANCELLED),
        APPROVED,       Set.of(REFUNDING, CANCELLED),
        REFUNDING,      Set.of(COMPLETED, FAILED)
    );

    /**
     * Returns {@code true} if transitioning from {@code this} to {@code next}
     * is legal for the given {@link RmaType}.
     */
    public boolean canTransitionTo(RmaStatus next, RmaType type) {
        Map<RmaStatus, Set<RmaStatus>> table = switch (type) {
            case RETURN      -> RETURN_TRANSITIONS;
            case EXCHANGE    -> EXCHANGE_TRANSITIONS;
            case REFUND_ONLY -> REFUND_ONLY_TRANSITIONS;
        };
        return table.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED || this == CANCELLED || this == FAILED;
    }
}
