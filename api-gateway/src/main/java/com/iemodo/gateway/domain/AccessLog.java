package com.iemodo.gateway.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Gateway access log entity.
 * 
 * <p>Maps to the {@code gateway_access_logs} table (partitioned by month).
 * Records all requests passing through the gateway for monitoring and analysis.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("gateway_access_logs")
public class AccessLog extends BaseEntity {

    // id is inherited from BaseEntity

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

}
