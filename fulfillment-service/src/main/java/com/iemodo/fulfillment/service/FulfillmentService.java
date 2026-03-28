package com.iemodo.fulfillment.service;

import com.iemodo.fulfillment.domain.StockTransferRecommendation;
import com.iemodo.fulfillment.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Fulfillment service interface
 */
public interface FulfillmentService {

    /**
     * Allocate warehouse for order
     */
    Mono<WarehouseAllocationResult> allocateWarehouse(WarehouseAllocationRequest request, String tenantId);

    /**
     * Rank warehouses for a destination
     */
    Mono<List<WarehouseRankResult>> rankWarehouses(WarehouseRankRequest request, String tenantId);

    /**
     * Calculate delivery time estimate
     */
    Mono<DeliveryEstimateResult> calculateDeliveryEstimate(Long warehouseId, String destinationCountry, 
                                                           String destinationPostalCode);

    /**
     * Generate restock recommendations
     */
    Flux<StockTransferRecommendation> generateRestockRecommendations(StockTransferRecommendation.AnalysisType analysisType, 
                                                                      Integer lookbackDays, String tenantId);

    /**
     * Execute stock transfer
     */
    Mono<StockTransferResult> executeStockTransfer(Long recommendationId, Long userId);

    /**
     * Get pending recommendations
     */
    Flux<StockTransferRecommendation> getPendingRecommendations(String tenantId);
}
