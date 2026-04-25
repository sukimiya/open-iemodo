package com.iemodo.marketing.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Coupon entity
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("coupons")
public class Coupon extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("coupon_code")
    private String couponCode;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("coupon_type")
    private CouponType type;

    @Column("discount_value")
    private BigDecimal discountValue;

    @Column("max_discount_amount")
    private BigDecimal maxDiscountAmount;

    @Column("min_order_amount")
    private BigDecimal minOrderAmount;

    @Column("max_uses")
    private Integer maxUses;

    @Column("max_uses_per_user")
    private Integer maxUsesPerUser;

    @Column("used_count")
    private Integer usedCount;

    @Column("applicable_scope")
    private ApplicableScope applicableScope;

    @Column("applicable_ids")
    private Long[] applicableIds;

    @Column("excluded_ids")
    private Long[] excludedIds;

    @Column("valid_from")
    private Instant validFrom;

    @Column("valid_to")
    private Instant validTo;

    @Column("coupon_active")
    private Boolean isActive;

    @Column("tenant_id")
    private String tenantId;

    // createTime, updateTime, createBy, updateBy are inherited from BaseEntity

    public enum CouponType {
        PERCENTAGE,      // Percentage discount (e.g., 20% off)
        FIXED_AMOUNT,    // Fixed amount discount (e.g., $10 off)
        FREE_SHIPPING    // Free shipping
    }

    public enum ApplicableScope {
        ALL,         // All products
        CATEGORIES,  // Specific categories
        PRODUCTS,    // Specific products
        BRANDS       // Specific brands
    }

    /**
     * Check if coupon is currently valid
     */
    public boolean isValid() {
        if (!Boolean.TRUE.equals(isActive)) return false;
        
        Instant now = Instant.now();
        if (validFrom != null && now.isBefore(validFrom)) return false;
        if (validTo != null && now.isAfter(validTo)) return false;
        
        // Check max uses
        if (maxUses != null && usedCount != null && usedCount >= maxUses) return false;
        
        return true;
    }

    /**
     * Calculate discount amount
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid()) return BigDecimal.ZERO;
        
        // Check minimum order amount
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        
        if (type == CouponType.FREE_SHIPPING) {
            // Return shipping cost (would be calculated separately)
            return BigDecimal.ZERO; // Placeholder
        }
        
        if (type == CouponType.FIXED_AMOUNT) {
            return discountValue.min(orderAmount);
        }
        
        if (type == CouponType.PERCENTAGE) {
            // discountValue is a percentage (e.g. 20 = 20%), divide by 100 before multiplying
            BigDecimal discount = orderAmount.multiply(
                    discountValue.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            if (maxDiscountAmount != null) {
                discount = discount.min(maxDiscountAmount);
            }
            return discount.setScale(2, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Increment used count
     */
    public void incrementUsedCount() {
        if (usedCount == null) {
            usedCount = 0;
        }
        usedCount++;
    }
}
