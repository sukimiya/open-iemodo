package com.iemodo.tenant.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Tenant data transfer object.
 */
@Data
@Builder
public class TenantDTO {

    private Long id;
    private String tenantId;
    private String tenantName;
    private String tenantCode;
    private String status;
    private String planType;
    private String contactEmail;
    private String contactPhone;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Nested configurations (optional)
    private Map<String, String> configs;
}
