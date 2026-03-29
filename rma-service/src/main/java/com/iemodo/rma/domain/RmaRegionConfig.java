package com.iemodo.rma.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Region-level RMA policy configuration.
 *
 * <p>A {@code null} tenantId means this is the platform-wide default for the region.
 * Tenant-specific rows (same regionCode, non-null tenantId) take precedence over defaults.
 *
 * <p>Examples:
 * <ul>
 *   <li>EU defaults: 14-day window, SELLER shipping, IF_NOT_SHIPPED tax refund</li>
 *   <li>US defaults: 30-day window, NEGOTIABLE shipping</li>
 * </ul>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("rma_region_configs")
public class RmaRegionConfig extends BaseEntity {

    /** e.g. EU, US, JP, AU — logical region code, not physical. */
    private String regionCode;

    /** null = platform default; non-null = tenant override. */
    private String tenantId;

    /** Days from delivery (or shipment if no delivery record) within which RETURN is allowed. */
    private Integer returnWindowDays;

    /** Days within which EXCHANGE is allowed. */
    private Integer exchangeWindowDays;

    /**
     * Who bears the return shipping cost.
     * SELLER | BUYER | NEGOTIABLE
     */
    private String shippingResponsibility;

    /**
     * When to include import tax in the refund amount.
     * NEVER | IF_NOT_SHIPPED | ALWAYS
     */
    private String taxRefundPolicy;

    /** Whether customers must provide a reason. */
    private Boolean requireReason;

    /**
     * Orders below this amount are auto-approved without merchant review.
     * null means no auto-approval.
     */
    private BigDecimal autoApproveThreshold;

    /** Currency for autoApproveThreshold. */
    private String autoApproveCurrency;
}
