package com.iemodo.fulfillment.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.fulfillment.domain.CustomsClearanceRule;
import com.iemodo.fulfillment.domain.StockTransferRecommendation;
import com.iemodo.fulfillment.dto.*;
import com.iemodo.fulfillment.repository.CustomsClearanceRuleRepository;
import com.iemodo.fulfillment.repository.StockTransferRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Fulfillment service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentServiceImpl implements FulfillmentService {

    private final StockTransferRecommendationRepository recommendationRepository;
    private final CustomsClearanceRuleRepository customsRuleRepository;

    // Mock warehouse data
    private final List<MockWarehouse> mockWarehouses = Arrays.asList(
            new MockWarehouse(1L, "WH-BJ", "Beijing Warehouse", "CN", "BJ", 39.9042, 116.4074, new BigDecimal("10.00"), 24, 0.95),
            new MockWarehouse(2L, "WH-SH", "Shanghai Warehouse", "CN", "SH", 31.2304, 121.4737, new BigDecimal("12.00"), 24, 0.98),
            new MockWarehouse(3L, "WH-GZ", "Guangzhou Warehouse", "CN", "GD", 23.1291, 113.2644, new BigDecimal("11.00"), 48, 0.92),
            new MockWarehouse(4L, "WH-LA", "Los Angeles Warehouse", "US", "CA", 34.0522, -118.2437, new BigDecimal("15.00"), 24, 0.96),
            new MockWarehouse(5L, "WH-NY", "New York Warehouse", "US", "NY", 40.7128, -74.0060, new BigDecimal("14.00"), 24, 0.94)
    );

    @Override
    public Mono<WarehouseAllocationResult> allocateWarehouse(WarehouseAllocationRequest request, String tenantId) {
        log.info("Allocating warehouse for order: {}, destination: {}", 
                request.getOrderId(), request.getDestinationCountry());
        
        return rankWarehouses(new WarehouseRankRequest(
                request.getDestinationCountry(),
                request.getDestinationPostalCode(),
                request.getItems()
        ), tenantId)
        .map(rankings -> {
            if (rankings.isEmpty()) {
                throw new RuntimeException("No suitable warehouse found");
            }
            
            // Get best warehouse
            WarehouseRankResult best = rankings.get(0);
            
            return WarehouseAllocationResult.builder()
                    .orderId(request.getOrderId())
                    .allocatedWarehouseId(best.getWarehouseId())
                    .warehouseName(best.getWarehouseName())
                    .warehouseCode(best.getWarehouseCode())
                    .shippingCost(best.getShippingCost())
                    .estimatedDeliveryDays(best.getEstimatedDeliveryDays())
                    .shippingMethod("STANDARD")
                    .items(request.getItems().stream()
                            .map(item -> WarehouseAllocationResult.AllocatedItem.builder()
                                    .sku(item.getSku())
                                    .quantity(item.getQuantity())
                                    .available(true)
                                    .build())
                            .toList())
                    .score(WarehouseAllocationResult.AllocationScore.builder()
                            .compositeScore(best.getCompositeScore())
                            .costScore(best.getCostScore())
                            .speedScore(best.getSpeedScore())
                            .availabilityScore(best.getAvailabilityScore())
                            .serviceLevelScore(best.getServiceLevelScore())
                            .build())
                    .build();
        });
    }

    @Override
    public Mono<List<WarehouseRankResult>> rankWarehouses(WarehouseRankRequest request, String tenantId) {
        String destinationCountry = request.getDestinationCountry().toUpperCase();
        
        // Calculate scores for each warehouse
        List<WarehouseRankResult> rankings = new ArrayList<>();
        
        for (MockWarehouse warehouse : mockWarehouses) {
            // Calculate distance
            double distance = calculateDistance(
                    warehouse.getLatitude(), warehouse.getLongitude(),
                    39.9042, 116.4074 // Mock destination (Beijing)
            );
            
            // Calculate delivery time
            int deliveryDays = calculateDeliveryDays(warehouse.getCountryCode(), destinationCountry, distance);
            
            // Calculate scores (0-1 scale)
            BigDecimal costScore = calculateCostScore(warehouse.getBaseShippingCost());
            BigDecimal speedScore = calculateSpeedScore(deliveryDays);
            BigDecimal availabilityScore = BigDecimal.valueOf(warehouse.getStockAvailability());
            BigDecimal serviceLevelScore = BigDecimal.valueOf(warehouse.getServiceLevel());
            
            // Calculate composite score (weighted average)
            BigDecimal compositeScore = costScore.multiply(new BigDecimal("0.25"))
                    .add(speedScore.multiply(new BigDecimal("0.35")))
                    .add(availabilityScore.multiply(new BigDecimal("0.25")))
                    .add(serviceLevelScore.multiply(new BigDecimal("0.15")))
                    .setScale(4, RoundingMode.HALF_UP);
            
            rankings.add(WarehouseRankResult.builder()
                    .warehouseId(warehouse.getId())
                    .warehouseName(warehouse.getName())
                    .warehouseCode(warehouse.getCode())
                    .compositeScore(compositeScore)
                    .costScore(costScore)
                    .speedScore(speedScore)
                    .availabilityScore(availabilityScore)
                    .serviceLevelScore(serviceLevelScore)
                    .shippingCost(warehouse.getBaseShippingCost())
                    .estimatedDeliveryDays(deliveryDays)
                    .distanceKm(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        
        // Sort by composite score (descending)
        rankings.sort(Comparator.comparing(WarehouseRankResult::getCompositeScore).reversed());
        
        return Mono.just(rankings);
    }

    @Override
    public Mono<DeliveryEstimateResult> calculateDeliveryEstimate(Long warehouseId, String destinationCountry, 
                                                                   String destinationPostalCode) {
        return customsRuleRepository.findByCountries(mockWarehouses.get(0).getCountryCode(), destinationCountry)
                .defaultIfEmpty(CustomsClearanceRule.builder()
                        .clearanceHours(48)
                        .isSameCountry(false)
                        .isCustomsUnion(false)
                        .isActive(true)
                        .build())
                .map(rule -> {
                    int customsHours = rule.getClearanceHours();
                    int transitDays = 3; // Mock transit time
                    int processingDays = 1; // Warehouse processing
                    
                    int totalDays = processingDays + (customsHours / 24) + transitDays;
                    
                    return DeliveryEstimateResult.builder()
                            .warehouseId(warehouseId)
                            .destinationCountry(destinationCountry)
                            .estimatedDays(totalDays)
                            .warehouseProcessingDays(processingDays)
                            .customsClearanceDays(customsHours / 24)
                            .transitDays(transitDays)
                            .build();
                });
    }

    @Override
    @Transactional
    public Flux<StockTransferRecommendation> generateRestockRecommendations(
            StockTransferRecommendation.AnalysisType analysisType, 
            Integer lookbackDays,
            String tenantId) {
        
        log.info("Generating restock recommendations: type={}, lookback={}", analysisType, lookbackDays);
        
        // Mock recommendations
        List<StockTransferRecommendation> recommendations = Arrays.asList(
                createRecommendation("REC-001", 3L, 1L, "SKU-001", 100, 
                        "High demand in Beijing region", StockTransferRecommendation.Priority.HIGH, tenantId),
                createRecommendation("REC-002", 4L, 5L, "SKU-002", 50, 
                        "Stock imbalance", StockTransferRecommendation.Priority.MEDIUM, tenantId)
        );
        
        return Flux.fromIterable(recommendations)
                .flatMap(recommendationRepository::save);
    }

    @Override
    @Transactional
    public Mono<StockTransferResult> executeStockTransfer(Long recommendationId, Long userId) {
        return recommendationRepository.findById(recommendationId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Recommendation not found")))
                .flatMap(recommendation -> {
                    if (recommendation.getRecommendationStatus() != StockTransferRecommendation.RecommendationStatus.APPROVED.getValue()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, 
                                "Recommendation must be approved before execution"));
                    }
                    
                    recommendation.markAsExecuted();
                    
                    return recommendationRepository.save(recommendation)
                            .map(r -> StockTransferResult.builder()
                                    .transferId(r.getId())
                                    .recommendationNo(r.getRecommendationNo())
                                    .fromWarehouseId(r.getFromWarehouseId())
                                    .toWarehouseId(r.getToWarehouseId())
                                    .sku(r.getSku())
                                    .quantity(r.getRecommendedQuantity())
                                    .status("EXECUTED")
                                    .executedAt(r.getExecutedAt())
                                    .build());
                });
    }

    @Override
    public Flux<StockTransferRecommendation> getPendingRecommendations(String tenantId) {
        return recommendationRepository.findByRecommendationStatusAndTenantId(
                StockTransferRecommendation.RecommendationStatus.PENDING.getValue(), tenantId);
    }

    // ─── Helper methods ───────────────────────────────────────────────────

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // Earth's radius in kilometers
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    private int calculateDeliveryDays(String originCountry, String destinationCountry, double distance) {
        if (originCountry.equals(destinationCountry)) {
            return 1; // Domestic
        } else if (isEU(originCountry) && isEU(destinationCountry)) {
            return 2; // EU internal
        } else {
            return 5 + (int) (distance / 1000); // International
        }
    }

    private boolean isEU(String countryCode) {
        return Set.of("DE", "FR", "IT", "ES", "NL", "BE", "AT").contains(countryCode);
    }

    private BigDecimal calculateCostScore(BigDecimal cost) {
        // Lower cost = higher score
        BigDecimal maxCost = new BigDecimal("20.00");
        BigDecimal score = maxCost.subtract(cost).divide(maxCost, 2, RoundingMode.HALF_UP);
        return score.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private BigDecimal calculateSpeedScore(int deliveryDays) {
        // Fewer days = higher score
        BigDecimal maxDays = new BigDecimal("10");
        BigDecimal days = new BigDecimal(deliveryDays);
        BigDecimal score = maxDays.subtract(days).divide(maxDays, 2, RoundingMode.HALF_UP);
        return score.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private StockTransferRecommendation createRecommendation(String no, Long from, Long to, String sku, 
                                                               int qty, String reason, 
                                                               StockTransferRecommendation.Priority priority,
                                                               String tenantId) {
        return StockTransferRecommendation.builder()
                .recommendationNo(no)
                .fromWarehouseId(from)
                .toWarehouseId(to)
                .analysisType(StockTransferRecommendation.AnalysisType.SKU)
                .lookbackDays(30)
                .sku(sku)
                .recommendedQuantity(qty)
                .reason(reason)
                .priority(priority)
                .projectedCostSavings(new BigDecimal("100.00"))
                .projectedDeliveryImprovement(new BigDecimal("0.15"))
                .status(StockTransferRecommendation.RecommendationStatus.PENDING.getValue())
                .tenantId(tenantId)
                .build();
    }

    // Mock warehouse data class
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class MockWarehouse {
        private Long id;
        private String code;
        private String name;
        private String countryCode;
        private String regionCode;
        private double latitude;
        private double longitude;
        private BigDecimal baseShippingCost;
        private int processingHours;
        private double serviceLevel;
        
        public double getStockAvailability() {
            return 0.95; // Mock 95% availability
        }
    }
}
