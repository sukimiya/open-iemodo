package com.iemodo.review.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer review for a purchased product.
 *
 * <p>The UNIQUE constraint on (order_item_id, user_id) enforces the
 * "one review per purchased item" rule at the database level.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("reviews")
public class Review extends BaseEntity {

    private String tenantId;

    private Long productId;
    private Long skuId;

    /** Ties the review to a specific purchase — used for purchase verification. */
    private Long orderId;
    private Long orderItemId;

    private Long userId;

    /** 1–5 star rating. */
    private Integer rating;

    private String title;
    private String content;

    /** Comma-separated list of file-service URLs for images/videos. */
    private String mediaUrls;

    private ReviewStatus reviewStatus;

    /** Number of "helpful" upvotes from other customers. */
    private Integer helpfulCount;

    /** Timestamp when the review was approved by moderation. */
    private Instant approvedAt;

    @Transient
    private List<ReviewReply> replies = new ArrayList<>();

    public Instant getCreatedAt() { return getCreateTime(); }
    public Instant getUpdatedAt() { return getUpdateTime(); }
}
