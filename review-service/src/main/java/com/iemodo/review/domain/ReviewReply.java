package com.iemodo.review.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Merchant or platform reply to a customer review.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("review_replies")
public class ReviewReply extends BaseEntity {

    private Long reviewId;
    private Long replierId;

    /**
     * Who replied.
     * MERCHANT | PLATFORM
     */
    private String replierType;

    private String content;

    public Instant getCreatedAt() { return getCreateTime(); }
}
