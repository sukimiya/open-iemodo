package com.iemodo.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iemodo.common.exception.ErrorCode;
import lombok.Getter;

import java.time.Instant;

/**
 * Unified API response wrapper.
 *
 * <p>Format:
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... },
 *   "timestamp": "2026-03-21T10:30:00Z"
 * }
 * </pre>
 */
@Getter
public class Response<T> {

    private final int code;
    private final String message;
    private final T data;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp;

    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    // ─── Factory methods ───────────────────────────────────────────────────

    public static <T> Response<T> success(T data) {
        return new Response<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Response<T> success() {
        return new Response<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> Response<T> error(ErrorCode errorCode) {
        return new Response<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Response<T> error(ErrorCode errorCode, String message) {
        return new Response<>(errorCode.getCode(), message, null);
    }

    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }
}
