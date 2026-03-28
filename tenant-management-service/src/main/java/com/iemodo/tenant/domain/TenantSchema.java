package com.iemodo.tenant.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Tenant Schema mapping — maps to the {@code tenant_schemas} table.
 * 
 * <p>Tracks which database schema each service uses for a given tenant.
 * This enables flexible schema-per-service or shared-schema strategies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("tenant_schemas")
public class TenantSchema {

    @Id
    private Long id;

    /** Reference to tenant (tenants.tenant_id) */
    private String tenantId;

    /** Service name: user-auth | product | order | inventory | payment | pricing | tax | marketing | fulfillment */
    private String serviceName;

    /** Actual database schema name (e.g., "acme_user_auth", "acme_product") */
    private String schemaName;

    /** Database connection pool name (for multi-database setups) */
    private String connectionPool;

    @CreatedDate
    private Instant createdAt;

    // ─── Convenience factory methods ──────────────────────────────────────

    public static TenantSchema of(String tenantId, String serviceName, String schemaName) {
        return TenantSchema.builder()
                .tenantId(tenantId)
                .serviceName(serviceName)
                .schemaName(schemaName)
                .build();
    }
}
