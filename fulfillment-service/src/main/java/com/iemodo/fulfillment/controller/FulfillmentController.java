package com.iemodo.fulfillment.controller;

import com.iemodo.common.response.Response;
import com.iemodo.fulfillment.domain.StockTransferRecommendation;
import com.iemodo.fulfillment.dto.*;
import com.iemodo.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Fulfillment controller
 */
@Slf4j
@RestController
@RequestMapping("/fulfillment/api/v1")
@RequiredArgsConstructor
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    /**
     * Allocate warehouse for order
     */
    @PostMapping("/warehouses/allocate")
    public Mono<Response<WarehouseAllocationResult>> allocateWarehouse(
            @Valid @RequestBody WarehouseAllocationRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return fulfillmentService.allocateWarehouse(request, tenantId)
                .map(Response::success);
    }

    /**
     * Rank warehouses for destination
     */
    @PostMapping("/warehouses/rank")
    public Mono<Response<List<WarehouseRankResult>>> rankWarehouses(
            @RequestBody WarehouseRankRequest request,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return fulfillmentService.rankWarehouses(request, tenantId)
                .map(Response::success);
    }

    /**
     * Calculate delivery estimate
     */
    @GetMapping("/delivery-estimate")
    public Mono<Response<DeliveryEstimateResult>> calculateDeliveryEstimate(
            @RequestParam Long warehouseId,
            @RequestParam String destinationCountry,
            @RequestParam(required = false) String destinationPostalCode) {
        
        return fulfillmentService.calculateDeliveryEstimate(warehouseId, destinationCountry, destinationPostalCode)
                .map(Response::success);
    }

    /**
     * Generate restock recommendations
     */
    @GetMapping("/restock/recommendations")
    public Mono<Response<List<StockTransferRecommendation>>> generateRestockRecommendations(
            @RequestParam StockTransferRecommendation.AnalysisType analysisType,
            @RequestParam(defaultValue = "30") Integer lookbackDays,
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return fulfillmentService.generateRestockRecommendations(analysisType, lookbackDays, tenantId)
                .collectList()
                .map(Response::success);
    }

    /**
     * Get pending recommendations
     */
    @GetMapping("/restock/recommendations/pending")
    public Mono<Response<List<StockTransferRecommendation>>> getPendingRecommendations(
            @RequestHeader(value = "X-TenantID", defaultValue = "tenant_001") String tenantId) {
        
        return fulfillmentService.getPendingRecommendations(tenantId)
                .collectList()
                .map(Response::success);
    }

    /**
     * Execute stock transfer
     */
    @PostMapping("/stock-transfers/{recommendationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Response<StockTransferResult>> executeStockTransfer(
            @PathVariable Long recommendationId,
            @RequestHeader(value = "X-UserID", defaultValue = "1") Long userId) {
        
        return fulfillmentService.executeStockTransfer(recommendationId, userId)
                .map(Response::success);
    }
}
