package com.iemodo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a concurrent request is detected
 * (e.g., duplicate submission, idempotency key conflict).
 */
public class ConcurrentRequestException extends BusinessException {

    public ConcurrentRequestException(String message) {
        super(ErrorCode.CONCURRENT_REQUEST, HttpStatus.CONFLICT, message);
    }

    public ConcurrentRequestException() {
        super(ErrorCode.CONCURRENT_REQUEST, HttpStatus.CONFLICT, 
                "Concurrent request detected, please retry");
    }
}
