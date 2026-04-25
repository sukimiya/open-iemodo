package com.iemodo.tenant.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/**
 * Daily usage record per tenant for metering and quota enforcement.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("usage_records")
public class UsageRecord extends BaseEntity {

    private String tenantId;

    /** Usage date */
    private LocalDate usageDate;

    /** Metric name: api_calls, products_created, orders_placed, storage_bytes */
    private String metric;

    /** Cumulative count for the day */
    private Long countValue;

    public void increment(long amount) {
        this.countValue = (countValue != null ? countValue : 0L) + amount;
    }
}
