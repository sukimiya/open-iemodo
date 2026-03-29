package com.iemodo.review.repository;

import com.iemodo.review.domain.ReviewReply;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ReviewReplyRepository extends ReactiveCrudRepository<ReviewReply, Long> {

    Flux<ReviewReply> findByReviewIdOrderByCreateTimeAsc(Long reviewId);
}
