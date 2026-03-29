package com.iemodo.review.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductRatingDTO {
    private Long productId;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private Integer fiveStar;
    private Integer fourStar;
    private Integer threeStar;
    private Integer twoStar;
    private Integer oneStar;
}
