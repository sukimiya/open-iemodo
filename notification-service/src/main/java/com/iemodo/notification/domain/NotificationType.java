package com.iemodo.notification.domain;

/**
 * Business event types that can trigger a notification.
 * Each type maps to one or more templates in notification_templates.
 */
public enum NotificationType {

    // ─── User ─────────────────────────────────────────────────────────────
    USER_REGISTERED,
    PASSWORD_RESET,

    // ─── Order ────────────────────────────────────────────────────────────
    ORDER_CREATED,
    ORDER_CANCELLED,

    // ─── Payment ──────────────────────────────────────────────────────────
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    REFUND_SUCCESS,

    // ─── Shipment ─────────────────────────────────────────────────────────
    ORDER_SHIPPED,
    ORDER_DELIVERED,

    // ─── RMA ──────────────────────────────────────────────────────────────
    RMA_APPROVED,
    RMA_REJECTED,
    RMA_RECEIVED,
    RMA_COMPLETED,

    // ─── Review ───────────────────────────────────────────────────────────
    REVIEW_APPROVED,
    REVIEW_REJECTED,

    // ─── Marketing ────────────────────────────────────────────────────────
    COUPON_EXPIRING,
    WISHLIST_PRICE_DROP,

    // ─── Billing ─────────────────────────────────────────────────────────
    USAGE_ALERT
}
