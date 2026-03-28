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
 * Quantity discount tier - volume-based discounts
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("quantity_discount_tiers")
public class QuantityDiscountTier extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("sku")
    private String sku;  // NULL = apply to all SKUs

    @Column("country_code")
    private String countryCode;  // NULL = apply to all countries

    @Column("min_quantity")
    private Integer minQuantity;

    @Column("max_quantity")
    private Integer maxQuantity;  // NULL = no upper limit

    @Column("discount_percent")
    private BigDecimal discountPercent;

    @Column("is_active")
    private Boolean isActive;

    @Column("effective_from")
    private Instant effectiveFrom;

    @Column("effective_to")
    private Instant effectiveTo;
}
