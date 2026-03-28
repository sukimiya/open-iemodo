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
import java.time.Instant;

/**
 * User coupon entity
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("user_coupons")
public class UserCoupon extends BaseEntity {
    // id is inherited from BaseEntity

    @Column("customer_id")
    private Long customerId;

    @Column("coupon_id")
    private Long couponId;

    @Column("coupon_code")
    private String couponCode;

    @Column("coupon_status")
    private UserCouponStatus couponStatus;

    @Column("order_id")
    private Long orderId;

    @Column("order_no")
    private String orderNo;

    @Column("used_at")
    private Instant usedAt;

    @Column("discount_amount")
    private BigDecimal discountAmount;

    @Column("valid_from")
    private Instant validFrom;

    @Column("valid_to")
    private Instant validTo;

    @Column("tenant_id")
    private String tenantId;

    public enum UserCouponStatus {
        UNUSED,
        USED,
        EXPIRED
    }

    /**
     * Check if user coupon is valid for use
     */
    public boolean isValid() {
        if (couponStatus != UserCouponStatus.UNUSED) return false;
        
        Instant now = Instant.now();
        if (validFrom != null && now.isBefore(validFrom)) return false;
        if (validTo != null && now.isAfter(validTo)) return false;
        
        return true;
    }

    /**
     * Mark as used
     */
    public void markAsUsed(Long orderId, String orderNo, BigDecimal discountAmount) {
        this.couponStatus = UserCouponStatus.USED;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.usedAt = Instant.now();
        this.discountAmount = discountAmount;
    }

    /**
     * Mark as expired
     */
    public void markAsExpired() {
        this.couponStatus = UserCouponStatus.EXPIRED;
    }
}
