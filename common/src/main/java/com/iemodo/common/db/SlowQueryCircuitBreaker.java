package com.iemodo.common.db;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rolling-window circuit breaker for slow database queries.
 *
 * <p>States and transitions:
 * <pre>
 *   CLOSED  ──(≥ threshold slow queries in window)──▶  OPEN
 *   OPEN    ──(recovery window elapsed)──────────────▶  HALF_OPEN
 *   HALF_OPEN ──(fast query succeeds)─────────────────▶  CLOSED
 *   HALF_OPEN ──(query is slow again)─────────────────▶  OPEN
 * </pre>
 *
 * <p>All state transitions are logged as {@code [CIRCUIT_BREAKER]} entries.
 * This class is thread-safe via volatile state + atomic counters.
 */
@Slf4j
public class SlowQueryCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state = State.CLOSED;
    private volatile long openedAtMs = 0;

    private final AtomicInteger windowSlowCount = new AtomicInteger(0);
    private final AtomicLong windowStartMs = new AtomicLong(System.currentTimeMillis());

    private final SlowQueryProperties props;

    public SlowQueryCircuitBreaker(SlowQueryProperties props) {
        this.props = props;
    }

    /**
     * Called after a query is measured as slow.
     * Logs the event and increments the window counter.
     * Opens the circuit if the threshold is reached.
     */
    public void recordSlowQuery(long elapsedMs, String sql) {
        log.warn("[SLOW_QUERY] elapsed={}ms sql={}", elapsedMs, abbreviate(sql));

        if (!props.isCircuitBreakerEnabled()) return;

        long now = System.currentTimeMillis();
        resetWindowIfExpired(now);

        int count = windowSlowCount.incrementAndGet();

        if (state == State.CLOSED && count >= props.getCircuitOpenThreshold()) {
            state = State.OPEN;
            openedAtMs = now;
            log.error("[CIRCUIT_BREAKER] DB circuit OPEN — {} slow queries in {}s window",
                    count, props.getWindowSeconds());
        } else if (state == State.HALF_OPEN) {
            // Probe request was also slow — back to OPEN
            state = State.OPEN;
            openedAtMs = now;
            log.warn("[CIRCUIT_BREAKER] DB circuit OPEN (HALF_OPEN probe was slow)");
        }
    }

    /**
     * Called after a query completes successfully and within the threshold.
     * Closes the circuit if it was in HALF_OPEN state.
     */
    public void recordFastQuery() {
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            windowSlowCount.set(0);
            windowStartMs.set(System.currentTimeMillis());
            log.info("[CIRCUIT_BREAKER] DB circuit CLOSED (recovered)");
        }
    }

    /**
     * Checks whether the circuit is open and throws {@link SlowQueryCircuitOpenException}
     * if a new DB connection should be rejected.
     *
     * <p>Transitions OPEN → HALF_OPEN when the recovery window has elapsed.
     */
    public void checkAndThrowIfOpen() {
        if (state == State.CLOSED) return;

        if (state == State.OPEN) {
            long now = System.currentTimeMillis();
            if (now - openedAtMs >= props.getRecoverySeconds() * 1000L) {
                state = State.HALF_OPEN;
                log.info("[CIRCUIT_BREAKER] DB circuit HALF_OPEN — probing recovery");
                return; // allow one probe through
            }
            long remainingMs = (props.getRecoverySeconds() * 1000L) - (now - openedAtMs);
            throw new SlowQueryCircuitOpenException(
                    "DB circuit breaker is OPEN, retry in " + (remainingMs / 1000) + "s");
        }
        // HALF_OPEN: let the probe through
    }

    public State getState() {
        return state;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private void resetWindowIfExpired(long now) {
        long windowStart = windowStartMs.get();
        if (now - windowStart >= props.getWindowSeconds() * 1000L) {
            if (windowStartMs.compareAndSet(windowStart, now)) {
                windowSlowCount.set(0);
            }
        }
    }

    private static String abbreviate(String sql) {
        if (sql == null) return "null";
        return sql.length() > 120 ? sql.substring(0, 120) + "…" : sql;
    }
}
