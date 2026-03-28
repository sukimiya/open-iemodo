package com.iemodo.tenant.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Tenant entity — maps to the {@code tenants} table in the {@code tenant_meta} schema.
 * 
 * <p>Platform-level table managing all tenants in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("tenants")
public class Tenant {

    @Id
    private Long id;

    /** Unique tenant identifier (e.g., "acme-corp", "demo-tenant") */
    private String tenantId;

    /** Human-readable tenant name */
    private String tenantName;

    /** Unique tenant code for API/subdomain usage */
    private String tenantCode;

    /** ACTIVE | SUSPENDED | DELETED */
    private String status;

    /** Plan type: STANDARD | PROFESSIONAL | ENTERPRISE */
    private String planType;

    /** Primary contact email for the tenant */
    private String contactEmail;

    /** Primary contact phone */
    private String contactPhone;

    /** Database host for this tenant's schemas */
    private String dbHost;

    /** Database name */
    private String dbName;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt;

    // ─── Domain behaviour ─────────────────────────────────────────────────

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void suspend() {
        this.status = "SUSPENDED";
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void softDelete() {
        this.status = "DELETED";
        this.deletedAt = Instant.now();
    }
}
