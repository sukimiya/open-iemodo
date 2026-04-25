package com.iemodo.tenant.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Tenant entity — maps to the {@code tenants} table in the {@code tenant_meta} schema.
 * 
 * <p>Platform-level table managing all tenants in the system.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("tenants")
public class Tenant extends BaseEntity {

    // id is inherited from BaseEntity

    /** Unique tenant identifier (e.g., "acme-corp", "demo-tenant") */
    private String tenantId;

    /** Human-readable tenant name */
    private String tenantName;

    /** Unique tenant code for API/subdomain usage */
    private String tenantCode;

    /** ACTIVE | SUSPENDED | DELETED */
    private String tenantStatus;

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

    // ─── Compatibility methods ────────────────────────────────────────────

    /**
     * 兼容方法：获取创建时间（返回 BaseEntity 的 createTime）
     */
    public Instant getCreatedAt() {
        return getCreateTime();
    }

    /**
     * 兼容方法：获取更新时间（返回 BaseEntity 的 updateTime）
     */
    public Instant getUpdatedAt() {
        return getUpdateTime();
    }

    // ─── Domain behaviour ─────────────────────────────────────────────────

    public boolean isActive() {
        return "ACTIVE".equals(tenantStatus);
    }

    public void suspend() {
        this.tenantStatus = "SUSPENDED";
    }

    public void activate() {
        this.tenantStatus = "ACTIVE";
    }

    public void softDelete() {
        this.tenantStatus = "DELETED";
        setIsValid(false);
    }
}
