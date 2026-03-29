package com.iemodo.review.dto;

import com.iemodo.review.domain.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ReviewDTO {

    private Long id;
    private String tenantId;
    private Long productId;
    private Long orderId;
    private Long orderItemId;
    private Long userId;

    private Integer rating;
    private String title;
    private String content;
    private List<String> mediaUrls;

    private ReviewStatus reviewStatus;
    private Integer helpfulCount;

    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private List<ReviewReplyDTO> replies;

    @Data
    @Builder
    public static class ReviewReplyDTO {
        private Long id;
        private Long replierId;
        private String replierType;
        private String content;
        private Instant createdAt;
    }
}
