package com.iemodo.review.repository;

import com.iemodo.review.domain.Review;
import com.iemodo.review.domain.ReviewStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewRepository extends ReactiveCrudRepository<Review, Long> {

    /** Public-facing: only APPROVED reviews ordered newest first. */
    Flux<Review> findByProductIdAndReviewStatusOrderByCreateTimeDesc(
            Long productId, ReviewStatus status, Pageable pageable);

    /** Check if a user already reviewed this specific order item. */
    Mono<Boolean> existsByOrderItemIdAndUserId(Long orderItemId, Long userId);

    Flux<Review> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Flux<Review> findByReviewStatusOrderByCreateTimeAsc(ReviewStatus status, Pageable pageable);

    /** Increment helpfulCount atomically. */
    @Query("UPDATE reviews SET helpful_count = helpful_count + 1 WHERE id = :id")
    Mono<Integer> incrementHelpfulCount(Long id);
}
