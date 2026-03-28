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
 * Tenant Configuration — maps to the {@code tenant_configs} table.
 * 
 * <p>Key-value store for tenant-specific settings like:
 * <ul>
 *   <li>default_currency = USD
 *   <li>default_language = en
 *   <li>tax_rate = 0.08
 *   <li>feature_flag_x = true
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("tenant_configs")
public class TenantConfig {

    @Id
    private Long id;

    private String tenantId;

    /** Configuration key (e.g., "default.currency", "tax.rate") */
    private String configKey;

    /** Configuration value as string (parsed based on type) */
    private String configValue;

    /** Value type: STRING | INTEGER | DECIMAL | BOOLEAN | JSON */
    private String configType;

    /** Description of what this config controls */
    private String description;

    /** Whether this config can be modified by the tenant admin */
    private Boolean editable;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ─── Common config keys ───────────────────────────────────────────────

    public static final String DEFAULT_CURRENCY = "default.currency";
    public static final String DEFAULT_LANGUAGE = "default.language";
    public static final String DEFAULT_COUNTRY = "default.country";
    public static final String TAX_RATE = "tax.rate";
    public static final String TIMEZONE = "timezone";
    public static final String DATE_FORMAT = "date.format";
    public static final String LOGO_URL = "brand.logo.url";
    public static final String PRIMARY_COLOR = "brand.primary.color";
}
