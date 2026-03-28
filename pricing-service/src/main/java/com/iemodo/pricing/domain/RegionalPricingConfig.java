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
 * Regional pricing configuration for different markets
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("regional_pricing_config")
public class RegionalPricingConfig extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("country_code")
    private String countryCode;  // ISO 3166-1 alpha-2

    @Column("sku")
    private String sku;  // NULL = default for country

    @Column("currency_code")
    private String currencyCode;

    @Column("markup_multiplier")
    private BigDecimal markupMultiplier;  // e.g., 1.15 = 15% markup

    @Column("discount_multiplier")
    private BigDecimal discountMultiplier;  // e.g., 0.90 = 10% discount

    @Column("min_price")
    private BigDecimal minPrice;

    @Column("max_price")
    private BigDecimal maxPrice;

    @Column("pricing_strategy")
    private PricingStrategy pricingStrategy;

    @Column("effective_from")
    private Instant effectiveFrom;

    @Column("effective_to")
    private Instant effectiveTo;

    @Column("is_active")
    private Boolean isActive;

    @Column("priority")
    private Integer priority;  // Higher = more specific

    /**
     * Check if config is currently effective
     */
    public boolean isEffective() {
        if (!Boolean.TRUE.equals(isActive)) return false;
        
        Instant now = Instant.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) return false;
        if (effectiveTo != null && now.isAfter(effectiveTo)) return false;
        
        return true;
    }

    /**
     * Calculate adjusted price
     */
    public BigDecimal applyMarkup(BigDecimal basePrice) {
        if (markupMultiplier != null) {
            return basePrice.multiply(markupMultiplier);
        }
        return basePrice;
    }

    /**
     * Apply discount
     */
    public BigDecimal applyDiscount(BigDecimal price) {
        if (discountMultiplier != null) {
            return price.multiply(discountMultiplier);
        }
        return price;
    }

    public enum PricingStrategy {
        STANDARD,     // Standard pricing with markup
        DYNAMIC,      // Dynamic pricing based on demand
        COMPETITIVE   // Competitor-based pricing
    }
}
