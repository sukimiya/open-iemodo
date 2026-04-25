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
 * Tracks billing usage alerts sent to tenants.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("usage_alert_log")
public class UsageAlertLog extends BaseEntity {

    private String tenantId;
    private String metric;
    private Integer thresholdPct;
    private Long currentUsage;
    private Long limitValue;
    private Instant alertSentAt;
    private String notifiedVia;
}
