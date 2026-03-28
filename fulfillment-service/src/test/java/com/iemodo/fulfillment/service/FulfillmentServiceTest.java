package com.iemodo.fulfillment.service;

import com.iemodo.fulfillment.domain.CustomsClearanceRule;
import com.iemodo.fulfillment.domain.StockTransferRecommendation;
import com.iemodo.fulfillment.dto.*;
import com.iemodo.fulfillment.repository.CustomsClearanceRuleRepository;
import com.iemodo.fulfillment.repository.StockTransferRecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Fulfillment service unit tests
 */
@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {

    @Mock
    private StockTransferRecommendationRepository recommendationRepository;

    @Mock
    private CustomsClearanceRuleRepository customsRuleRepository;

    @InjectMocks
    private FulfillmentServiceImpl fulfillmentService;

    @Test
    void rankWarehouses_Success() {
        WarehouseRankRequest request = new WarehouseRankRequest();
        request.setDestinationCountry("CN");
        request.setDestinationPostalCode("100000");
        
        StepVerifier.create(fulfillmentService.rankWarehouses(request, "tenant_001"))
                .expectNextMatches(rankings -> 
                        !rankings.isEmpty() && 
                        rankings.get(0).getCompositeScore() != null)
                .verifyComplete();
    }

    @Test
    void allocateWarehouse_Success() {
        WarehouseAllocationRequest request = WarehouseAllocationRequest.builder()
                .orderId("ORD-001")
                .destinationCountry("CN")
                .destinationPostalCode("100000")
                .items(List.of(
                        new WarehouseAllocationRequest.AllocationItem("SKU-001", 2)
                ))
                .preference(WarehouseAllocationRequest.AllocationPreference.BALANCED)
                .build();
        
        StepVerifier.create(fulfillmentService.allocateWarehouse(request, "tenant_001"))
                .expectNextMatches(result -> 
                        result.getOrderId().equals("ORD-001") &&
                        result.getAllocatedWarehouseId() != null &&
                        result.getScore() != null)
                .verifyComplete();
    }

    @Test
    void calculateDeliveryEstimate_Domestic() {
        CustomsClearanceRule rule = CustomsClearanceRule.builder()
                .originCountry("CN")
                .destinationCountry("CN")
                .clearanceHours(0)
                .isSameCountry(true)
                .isCustomsUnion(false)
                .build();
        
        when(customsRuleRepository.findByCountries(anyString(), anyString()))
                .thenReturn(Mono.just(rule));
        
        StepVerifier.create(fulfillmentService.calculateDeliveryEstimate(1L, "CN", "100000"))
                .expectNextMatches(result -> 
                        result.getEstimatedDays() > 0 &&
                        result.getWarehouseProcessingDays() >= 0)
                .verifyComplete();
    }

    @SuppressWarnings("null")
@Test
    void generateRestockRecommendations_Success() {
        StockTransferRecommendation rec = StockTransferRecommendation.builder()
                .recommendationNo("REC-001")
                .fromWarehouseId(1L)
                .toWarehouseId(2L)
                .sku("SKU-001")
                .recommendedQuantity(100)
                .build();
        
        when(recommendationRepository.save(any()))
                .thenReturn(Mono.just(rec));
        
        StepVerifier.create(fulfillmentService.generateRestockRecommendations(
                        StockTransferRecommendation.AnalysisType.SKU, 30, "tenant_001")
                .collectList())
                .expectNextMatches(list -> !list.isEmpty())
                .verifyComplete();
    }

    @SuppressWarnings("null")
@Test
    void executeStockTransfer_Success() {
        StockTransferRecommendation recommendation = StockTransferRecommendation.builder()
                .id(1L)
                .recommendationNo("REC-001")
                .fromWarehouseId(1L)
                .toWarehouseId(2L)
                .sku("SKU-001")
                .recommendedQuantity(100)
                .recommendationStatus(StockTransferRecommendation.RecommendationStatus.APPROVED.getValue())
                .build();
        
        when(recommendationRepository.findById(1L))
                .thenReturn(Mono.just(recommendation));
        when(recommendationRepository.save(any()))
                .thenReturn(Mono.just(recommendation));
        
        StepVerifier.create(fulfillmentService.executeStockTransfer(1L, 1L))
                .expectNextMatches(result -> 
                        result.getStatus().equals("EXECUTED"))
                .verifyComplete();
    }
}
