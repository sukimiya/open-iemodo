package com.iemodo.review.repository;

import com.iemodo.review.domain.ProductRatingSummary;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ProductRatingSummaryRepository
        extends ReactiveCrudRepository<ProductRatingSummary, Long> {

    Mono<ProductRatingSummary> findByProductIdAndTenantId(Long productId, String tenantId);
}
