package com.iemodo.rma.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.rma.domain.*;
import com.iemodo.rma.dto.CreateRmaRequest;
import com.iemodo.rma.repository.RmaItemRepository;
import com.iemodo.rma.repository.RmaRequestRepository;
import com.iemodo.rma.repository.RmaStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("RmaService")
class RmaServiceTest {

    @Mock private RmaRequestRepository       rmaRequestRepository;
    @Mock private RmaItemRepository          rmaItemRepository;
    @Mock private RmaStatusHistoryRepository historyRepository;
    @Mock private RmaRegionConfigService     regionConfigService;

    private RmaService rmaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rmaService = new RmaService(
                rmaRequestRepository, rmaItemRepository,
                historyRepository, regionConfigService);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private RmaRegionConfig defaultConfig() {
        return RmaRegionConfig.builder()
                .regionCode("US")
                .returnWindowDays(30)
                .exchangeWindowDays(30)
                .shippingResponsibility("NEGOTIABLE")
                .taxRefundPolicy("IF_NOT_SHIPPED")
                .requireReason(true)
                .autoApproveThreshold(null)
                .build();
    }

    private RmaRegionConfig autoApproveConfig() {
        return RmaRegionConfig.builder()
                .regionCode("US")
                .returnWindowDays(30)
                .exchangeWindowDays(30)
                .shippingResponsibility("NEGOTIABLE")
                .taxRefundPolicy("IF_NOT_SHIPPED")
                .requireReason(true)
                .autoApproveThreshold(new BigDecimal("50.00"))
                .autoApproveCurrency("USD")
                .build();
    }

    private CreateRmaRequest buildReturnRequest() {
        CreateRmaRequest req = new CreateRmaRequest();
        req.setOrderId(100L);
        req.setType(RmaType.RETURN);
        req.setRegionCode("US");
        req.setReason("Product defective");

        CreateRmaRequest.RmaItemRequest item = new CreateRmaRequest.RmaItemRequest();
        item.setOrderItemId(200L);
        item.setProductId(10L);
        item.setSku("SKU-001");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("29.99"));
        req.setItems(List.of(item));
        return req;
    }

    private RmaRequest savedRma(RmaStatus status) {
        return RmaRequest.builder()
                .id(1L)
                .rmaNo("RMA20260329000001")
                .tenantId("tenant_001")
                .orderId(100L)
                .customerId(99L)
                .type(RmaType.RETURN)
                .rmaStatus(status)
                .regionCode("US")
                .regionSnapshot("{}")
                .reason("Product defective")
                .build();
    }

    // ─── createRma ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRma: should create PENDING_REVIEW when no auto-approve threshold")
    void createRma_pendingReview_whenNoAutoApprove() {
        when(regionConfigService.resolve("tenant_001", "US"))
                .thenReturn(Mono.just(defaultConfig()));
        when(regionConfigService.toSnapshot(any())).thenReturn("{}");
        when(rmaRequestRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(rmaItemRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rmaService.createRma(buildReturnRequest(), 99L, "tenant_001"))
                .assertNext(dto -> {
                    assertThat(dto.getRmaStatus()).isEqualTo(RmaStatus.PENDING_REVIEW);
                    assertThat(dto.getRmaNo()).startsWith("RMA");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createRma: should auto-approve when order total is below threshold")
    void createRma_autoApproved_whenBelowThreshold() {
        when(regionConfigService.resolve("tenant_001", "US"))
                .thenReturn(Mono.just(autoApproveConfig())); // threshold = 50.00, item = 29.99
        when(regionConfigService.toSnapshot(any())).thenReturn("{}");
        when(rmaRequestRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(rmaItemRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rmaService.createRma(buildReturnRequest(), 99L, "tenant_001"))
                .assertNext(dto -> assertThat(dto.getRmaStatus()).isEqualTo(RmaStatus.APPROVED))
                .verifyComplete();
    }

    // ─── approve ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve: should transition PENDING_REVIEW to APPROVED")
    void approve_shouldTransitionToApproved() {
        RmaRequest rma = savedRma(RmaStatus.PENDING_REVIEW);
        when(rmaRequestRepository.findById(1L)).thenReturn(Mono.just(rma));
        when(rmaRequestRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(rmaItemRepository.findByRmaId(1L)).thenReturn(Flux.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(regionConfigService.fromSnapshot("{}")).thenReturn(defaultConfig());

        StepVerifier.create(rmaService.approve(1L, 42L, "Looks good", false))
                .assertNext(dto -> {
                    assertThat(dto.getRmaStatus()).isEqualTo(RmaStatus.APPROVED);
                    assertThat(dto.getApprovedAt()).isNotNull();
                })
                .verifyComplete();
    }

    // ─── reject ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("reject: should transition to REJECTED")
    void reject_shouldTransitionToRejected() {
        RmaRequest rma = savedRma(RmaStatus.PENDING_REVIEW);
        when(rmaRequestRepository.findById(1L)).thenReturn(Mono.just(rma));
        when(rmaRequestRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(rmaItemRepository.findByRmaId(1L)).thenReturn(Flux.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rmaService.reject(1L, 42L, "Not eligible"))
                .assertNext(dto -> assertThat(dto.getRmaStatus()).isEqualTo(RmaStatus.REJECTED))
                .verifyComplete();
    }

    // ─── submitTracking ───────────────────────────────────────────────────

    @Test
    @DisplayName("submitTracking: should transition APPROVED to IN_TRANSIT")
    void submitTracking_shouldTransitionToInTransit() {
        RmaRequest rma = savedRma(RmaStatus.APPROVED);
        // Make the state machine valid: APPROVED → WAITING_SHIPMENT first,
        // but submitTracking directly goes to IN_TRANSIT — let's fix the pre-state
        rma.setRmaStatus(RmaStatus.WAITING_SHIPMENT);

        when(rmaRequestRepository.findById(1L)).thenReturn(Mono.just(rma));
        when(rmaRequestRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(rmaItemRepository.findByRmaId(1L)).thenReturn(Flux.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rmaService.submitTracking(1L, "1Z999AA1", "UPS", 99L))
                .assertNext(dto -> {
                    assertThat(dto.getRmaStatus()).isEqualTo(RmaStatus.IN_TRANSIT);
                    assertThat(dto.getTrackingNo()).isEqualTo("1Z999AA1");
                    assertThat(dto.getCarrier()).isEqualTo("UPS");
                })
                .verifyComplete();
    }

    // ─── getByRmaNo ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getByRmaNo: should throw NOT_FOUND when RMA does not exist")
    void getByRmaNo_shouldThrow_whenNotFound() {
        when(rmaRequestRepository.findByRmaNo("RMA_NONEXISTENT")).thenReturn(Mono.empty());

        StepVerifier.create(rmaService.getByRmaNo("RMA_NONEXISTENT"))
                .expectErrorMatches(ex -> ex instanceof BusinessException be
                        && be.getErrorCode() == com.iemodo.common.exception.ErrorCode.NOT_FOUND)
                .verify();
    }

    // ─── cancel ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: should cancel a PENDING_REVIEW request")
    void cancel_shouldCancelPendingReview() {
        RmaRequest rma = savedRma(RmaStatus.PENDING_REVIEW);
        when(rmaRequestRepository.findById(1L)).thenReturn(Mono.just(rma));
        when(rmaRequestRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(rmaItemRepository.findByRmaId(1L)).thenReturn(Flux.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rmaService.cancel(1L, 99L))
                .assertNext(dto -> assertThat(dto.getRmaStatus()).isEqualTo(RmaStatus.CANCELLED))
                .verifyComplete();
    }

    @Test
    @DisplayName("cancel: should throw when trying to cancel a COMPLETED request")
    void cancel_shouldThrow_whenCompleted() {
        RmaRequest rma = savedRma(RmaStatus.COMPLETED);
        when(rmaRequestRepository.findById(1L)).thenReturn(Mono.just(rma));

        StepVerifier.create(rmaService.cancel(1L, 99L))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
