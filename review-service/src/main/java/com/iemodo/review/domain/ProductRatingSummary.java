package com.iemodo.review.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Denormalized rating aggregate per product — updated asynchronously
 * after each review is approved or removed.
 *
 * <p>Using productId as the primary key (not a surrogate) because
 * there is exactly one summary row per product per tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_rating_summary")
public class ProductRatingSummary {

    @Id
    private Long productId;

    private String tenantId;

    private BigDecimal avgRating;
    private Integer totalReviews;

    private Integer fiveStar;
    private Integer fourStar;
    private Integer threeStar;
    private Integer twoStar;
    private Integer oneStar;

    private Instant updatedAt;

    /** Recompute avgRating from the star counts. */
    public void recalcAvg() {
        int total = fiveStar + fourStar + threeStar + twoStar + oneStar;
        if (total == 0) {
            this.avgRating = BigDecimal.ZERO;
            this.totalReviews = 0;
            return;
        }
        int weighted = 5 * fiveStar + 4 * fourStar + 3 * threeStar + 2 * twoStar + oneStar;
        this.avgRating = BigDecimal.valueOf(weighted)
                .divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
        this.totalReviews = total;
        this.updatedAt = Instant.now();
    }

    /** Increment the bucket for the given star rating and recompute. */
    public void addRating(int stars) {
        switch (stars) {
            case 5 -> fiveStar++;
            case 4 -> fourStar++;
            case 3 -> threeStar++;
            case 2 -> twoStar++;
            case 1 -> oneStar++;
        }
        recalcAvg();
    }

    /** Decrement the bucket when a review is removed/rejected and recompute. */
    public void removeRating(int stars) {
        switch (stars) {
            case 5 -> fiveStar = Math.max(0, fiveStar - 1);
            case 4 -> fourStar = Math.max(0, fourStar - 1);
            case 3 -> threeStar = Math.max(0, threeStar - 1);
            case 2 -> twoStar = Math.max(0, twoStar - 1);
            case 1 -> oneStar = Math.max(0, oneStar - 1);
        }
        recalcAvg();
    }

    public static ProductRatingSummary empty(Long productId, String tenantId) {
        return ProductRatingSummary.builder()
                .productId(productId)
                .tenantId(tenantId)
                .avgRating(BigDecimal.ZERO)
                .totalReviews(0)
                .fiveStar(0).fourStar(0).threeStar(0).twoStar(0).oneStar(0)
                .updatedAt(Instant.now())
                .build();
    }
}
