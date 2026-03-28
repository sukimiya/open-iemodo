package com.iemodo.fulfillment.repository;

import com.iemodo.fulfillment.domain.StockTransferRecommendation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Stock transfer recommendation repository
 */
@Repository
public interface StockTransferRecommendationRepository extends ReactiveCrudRepository<StockTransferRecommendation, Long> {

    Flux<StockTransferRecommendation> findByRecommendationStatusAndTenantId(Integer recommendationStatus, String tenantId);

    Flux<StockTransferRecommendation> findByTenantId(String tenantId);
}
