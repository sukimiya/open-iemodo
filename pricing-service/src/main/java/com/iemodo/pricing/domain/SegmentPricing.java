package com.iemodo.pricing.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Customer segment pricing - VIP, wholesale, etc.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("segment_pricing")
public class SegmentPricing extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("segment_code")
    private String segmentCode;  // VIP, WHOLESALE, STUDENT, etc.

    @Column("sku")
    private String sku;  // NULL = apply to all

    @Column("discount_percent")
    private BigDecimal discountPercent;

    @Column("is_active")
    private Boolean isActive;

    @Column("effective_from")
    private Instant effectiveFrom;

    @Column("effective_to")
    private Instant effectiveTo;
}
