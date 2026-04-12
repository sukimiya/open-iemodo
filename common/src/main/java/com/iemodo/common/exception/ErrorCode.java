package com.iemodo.common.exception;

/**
 * Unified error codes aligned with PRD Section 6.3.
 */
public enum ErrorCode {

    // ─── Success ────────────────────────────────────────────
    SUCCESS(200, "success"),

    // ─── Client errors ──────────────────────────────────────
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource not found"),
    INSUFFICIENT_STOCK(409, "Insufficient stock"),

    // ─── Business errors ────────────────────────────────────
    TENANT_NOT_FOUND(4001, "Tenant not found"),
    TENANT_ID_MISSING(4002, "X-TenantID header is required"),
    USER_NOT_FOUND(4010, "User not found"),
    USER_ALREADY_EXISTS(4011, "User already exists"),
    INVALID_CREDENTIALS(4012, "Invalid email or password"),
    TOKEN_EXPIRED(4013, "Token has expired"),
    TOKEN_INVALID(4014, "Token is invalid"),
    ORDER_NOT_FOUND(4030, "Order not found"),
    INVALID_ORDER_STATUS(4031, "Invalid order status transition"),
    DUPLICATE_ORDER(4032, "Order already created for this idempotency key"),
    INVALID_IDEMPOTENCY_TOKEN(4033, "Idempotency token not found or expired"),
    INVALID_PAYMENT_STATUS(4034, "Invalid payment status transition"),
    RMA_NOT_FOUND(4040, "RMA request not found"),
    INVALID_RMA_TRANSITION(4041, "Invalid RMA status transition"),
    RETURN_WINDOW_EXPIRED(4042, "Return window has expired for this region"),
    RMA_ALREADY_EXISTS(4043, "An active RMA already exists for this order item"),

    // ─── Infrastructure errors ───────────────────────────────
    ENCRYPTION_ERROR(5001, "Encryption operation failed"),
    MAP_SERVICE_ERROR(5002, "Map service operation failed"),
    FILE_STORAGE_ERROR(5003, "File storage operation failed"),
    CACHE_ERROR(5004, "Cache operation failed"),

    // ─── Concurrency errors ─────────────────────────────────
    CONCURRENT_REQUEST(4090, "Concurrent request detected"),
    RATE_LIMIT_EXCEEDED(4290, "Rate limit exceeded"),

    // ─── Server errors ──────────────────────────────────────
    INTERNAL_ERROR(500, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
