package com.iemodo.common.db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for slow-query detection and DB circuit breaking.
 *
 * <pre>
 * iemodo:
 *   db:
 *     slow-query:
 *       threshold-ms: 500
 *       circuit-open-threshold: 10
 *       window-seconds: 60
 *       recovery-seconds: 30
 *       circuit-breaker-enabled: true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "iemodo.db.slow-query")
public class SlowQueryProperties {

    /** Queries taking longer than this are considered slow (ms). Default: 500 */
    private long thresholdMs = 500;

    /**
     * Number of slow queries in the window that triggers circuit open.
     * Default: 10
     */
    private int circuitOpenThreshold = 10;

    /** Rolling time window for the slow-query counter (seconds). Default: 60 */
    private int windowSeconds = 60;

    /** How long the circuit stays OPEN before probing with HALF_OPEN (seconds). Default: 30 */
    private int recoverySeconds = 30;

    /** Set to false to keep slow-query logging but disable circuit breaking. Default: true */
    private boolean circuitBreakerEnabled = true;
}
