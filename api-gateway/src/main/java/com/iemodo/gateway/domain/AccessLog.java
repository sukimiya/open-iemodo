package com.iemodo.gateway.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Gateway access log entity.
 * 
 * <p>Maps to the {@code gateway_access_logs} table (partitioned by month).
 * Records all requests passing through the gateway for monitoring and analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("gateway_access_logs")
public class AccessLog {

    @Id
    private Long id;

    /** Unique request ID */
    private String requestId;

    /** Distributed trace ID */
    private String traceId;

    /** Tenant identifier */
    private String tenantId;

    /** User ID if authenticated */
    private Long userId;

    /** HTTP method */
    private String method;

    /** Request path */
    private String path;

    /** Query parameters */
    private String queryParams;

    /** Client IP address */
    private String clientIp;

    /** User agent string */
    private String userAgent;

    /** HTTP response status code */
    private Integer statusCode;

    /** Response time in milliseconds */
    private Integer responseTime;

    /** Request body size in bytes */
    private Long requestSize;

    /** Response body size in bytes */
    private Long responseSize;

    /** Error message if any */
    private String errorMessage;

    /** Route ID that handled this request */
    private String routeId;

    /** Target URI */
    private String targetUri;

    /** Log timestamp */
    private Instant createdAt;

    // ─── Convenience factory method ────────────────────────────────────────

    public static AccessLogBuilder builder() {
        return new AccessLogBuilder()
                .createdAt(Instant.now());
    }
}
