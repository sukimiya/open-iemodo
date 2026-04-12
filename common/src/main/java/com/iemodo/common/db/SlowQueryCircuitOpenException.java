package com.iemodo.common.db;

/**
 * Thrown when the DB slow-query circuit breaker is OPEN and a new connection
 * is requested. Callers should treat this as a temporary service unavailability.
 */
public class SlowQueryCircuitOpenException extends RuntimeException {

    public SlowQueryCircuitOpenException(String message) {
        super(message);
    }
}
