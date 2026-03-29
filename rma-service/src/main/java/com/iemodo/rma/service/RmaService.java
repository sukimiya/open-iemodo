package com.iemodo.rma.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.rma.domain.*;
import com.iemodo.rma.dto.CreateRmaRequest;
import com.iemodo.rma.dto.RmaDTO;
import com.iemodo.rma.dto.RmaItemDTO;
import com.iemodo.rma.repository.RmaItemRepository;
import com.iemodo.rma.repository.RmaRequestRepository;
import com.iemodo.rma.repository.RmaStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RmaService {

    private final RmaRequestRepository       rmaRequestRepository;
    private final RmaItemRepository          rmaItemRepository;
    private final RmaStatusHistoryRepository historyRepository;
    private final RmaRegionConfigService     regionConfigService;

    private static final AtomicLong SEQ = new AtomicLong(0);

    // ─── Create ───────────────────────────────────────────────────────────

    @Transactional
    public Mono<RmaDTO> createRma(CreateRmaRequest req, Long customerId, String tenantId) {
        String regionCode = req.getRegionCode() != null ? req.getRegionCode() : "US";

        return regionConfigService.resolve(tenantId, regionCode)
                .flatMap(config -> {
                    // Validate return window using region snapshot
                    validateReturnWindow(req, config);

                    String snapshot = regionConfigService.toSnapshot(config);

                    RmaRequest rma = RmaRequest.builder()
                            .rmaNo(generateRmaNo())
                            .tenantId(tenantId)
                            .orderId(req.getOrderId())
                            .customerId(customerId)
                            .type(req.getType())
                            .rmaStatus(RmaStatus.PENDING_REVIEW)
                            .regionCode(regionCode)
                            .regionSnapshot(snapshot)
                            .reason(req.getReason())
                            .description(req.getDescription())
                            .build();

                    // Auto-approve if order total is below threshold
                    if (shouldAutoApprove(req, config)) {
                        BigDecimal refundAmount = calculateRefundAmount(req, config, false);
                        rma.transitionTo(RmaStatus.APPROVED, null);
                        rma.setRefundAmount(refundAmount);
                        rma.setRefundCurrency("USD"); // TODO: resolve from order currency
                        rma.setTaxRefundIncluded(false);
                        log.info("RMA {} auto-approved (below threshold {})",
                                rma.getRmaNo(), config.getAutoApproveThreshold());
                    }

                    return rmaRequestRepository.save(rma);
                })
                .flatMap(savedRma -> {
                    List<RmaItem> items = req.getItems().stream()
                            .map(i -> RmaItem.builder()
                                    .rmaId(savedRma.getId())
                                    .orderItemId(i.getOrderItemId())
                                    .productId(i.getProductId())
                                    .sku(i.getSku())
                                    .quantity(i.getQuantity())
                                    .unitPrice(i.getUnitPrice())
                                    .reason(i.getReason())
                                    .build())
                            .collect(Collectors.toList());

                    return rmaItemRepository.saveAll(items)
                            .collectList()
                            .flatMap(savedItems -> {
                                savedRma.setItems(savedItems);
                                // Record initial history entry
                                return recordHistory(savedRma.getId(), null,
                                        RmaStatus.PENDING_REVIEW, customerId, "CUSTOMER", "RMA created")
                                        .thenReturn(savedRma);
                            });
                })
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Created RMA={} type={} tenant={}",
                        dto.getRmaNo(), dto.getType(), tenantId));
    }

    // ─── Approve ──────────────────────────────────────────────────────────

    @Transactional
    public Mono<RmaDTO> approve(Long rmaId, Long operatorId, String notes,
                                boolean orderShipped) {
        return findById(rmaId)
                .flatMap(rma -> rmaItemRepository.findByRmaId(rmaId)
                        .collectList()
                        .flatMap(items -> {
                            RmaStatus before = rma.getRmaStatus();
                            rma.transitionTo(RmaStatus.APPROVED, operatorId);
                            rma.setMerchantNotes(notes);

                            // Calculate refund amount now that items are loaded
                            RmaRegionConfig config = regionConfigService.fromSnapshot(rma.getRegionSnapshot());
                            BigDecimal refundAmount = calculateRefundAmountFromItems(
                                    items, config, orderShipped);
                            rma.setRefundAmount(refundAmount);
                            rma.setTaxRefundIncluded(!orderShipped &&
                                    "IF_NOT_SHIPPED".equals(config.getTaxRefundPolicy()));
                            rma.setItems(items);

                            return rmaRequestRepository.save(rma)
                                    .flatMap(saved -> recordHistory(rmaId, before, RmaStatus.APPROVED,
                                            operatorId, "MERCHANT", notes)
                                            .thenReturn(saved));
                        }))
                .flatMap(this::loadItemsAndToDTO)
                .doOnSuccess(dto -> log.info("RMA={} approved by operator={}", dto.getRmaNo(), operatorId));
    }

    // ─── Reject ───────────────────────────────────────────────────────────

    @Transactional
    public Mono<RmaDTO> reject(Long rmaId, Long operatorId, String reason) {
        return findById(rmaId)
                .flatMap(rma -> {
                    RmaStatus before = rma.getRmaStatus();
                    rma.transitionTo(RmaStatus.REJECTED, operatorId);
                    rma.setMerchantNotes(reason);
                    return rmaRequestRepository.save(rma)
                            .flatMap(saved -> recordHistory(rmaId, before, RmaStatus.REJECTED,
                                    operatorId, "MERCHANT", reason)
                                    .thenReturn(saved));
                })
                .flatMap(this::loadItemsAndToDTO)
                .doOnSuccess(dto -> log.info("RMA={} rejected by operator={}", dto.getRmaNo(), operatorId));
    }

    // ─── Submit return tracking ───────────────────────────────────────────

    @Transactional
    public Mono<RmaDTO> submitTracking(Long rmaId, String trackingNo,
                                       String carrier, Long customerId) {
        return findById(rmaId)
                .flatMap(rma -> {
                    RmaStatus before = rma.getRmaStatus();
                    rma.transitionTo(RmaStatus.IN_TRANSIT, customerId);
                    rma.setTrackingNo(trackingNo);
                    rma.setCarrier(carrier);
                    return rmaRequestRepository.save(rma)
                            .flatMap(saved -> recordHistory(rmaId, before, RmaStatus.IN_TRANSIT,
                                    customerId, "CUSTOMER",
                                    "Tracking: " + trackingNo + " via " + carrier)
                                    .thenReturn(saved));
                })
                .flatMap(this::loadItemsAndToDTO);
    }

    // ─── Mark received ────────────────────────────────────────────────────

    @Transactional
    public Mono<RmaDTO> markReceived(Long rmaId, Long operatorId) {
        return findById(rmaId)
                .flatMap(rma -> {
                    RmaStatus before = rma.getRmaStatus();
                    rma.transitionTo(RmaStatus.RECEIVED, operatorId);
                    return rmaRequestRepository.save(rma)
                            .flatMap(saved -> recordHistory(rmaId, before, RmaStatus.RECEIVED,
                                    operatorId, "MERCHANT", "Goods received at warehouse")
                                    .thenReturn(saved));
                })
                .flatMap(this::loadItemsAndToDTO);
    }

    // ─── Inspection result (EXCHANGE only) ───────────────────────────────

    @Transactional
    public Mono<RmaDTO> inspect(Long rmaId, boolean passed, Long operatorId, String notes) {
        return findById(rmaId)
                .flatMap(rma -> {
                    RmaStatus before = rma.getRmaStatus();
                    // passed → reship; failed → refund instead
                    RmaStatus next = passed ? RmaStatus.RESHIPPING : RmaStatus.REFUNDING;
                    rma.transitionTo(next, operatorId);
                    rma.setMerchantNotes(notes);
                    return rmaRequestRepository.save(rma)
                            .flatMap(saved -> recordHistory(rmaId, before, next,
                                    operatorId, "MERCHANT",
                                    passed ? "Inspection passed" : "Inspection failed: " + notes)
                                    .thenReturn(saved));
                })
                .flatMap(this::loadItemsAndToDTO);
    }

    // ─── Cancel ───────────────────────────────────────────────────────────

    @Transactional
    public Mono<RmaDTO> cancel(Long rmaId, Long customerId) {
        return findById(rmaId)
                .flatMap(rma -> {
                    RmaStatus before = rma.getRmaStatus();
                    rma.transitionTo(RmaStatus.CANCELLED, customerId);
                    return rmaRequestRepository.save(rma)
                            .flatMap(saved -> recordHistory(rmaId, before, RmaStatus.CANCELLED,
                                    customerId, "CUSTOMER", "Customer cancelled")
                                    .thenReturn(saved));
                })
                .flatMap(this::loadItemsAndToDTO);
    }

    // ─── Query ────────────────────────────────────────────────────────────

    public Mono<RmaDTO> getByRmaNo(String rmaNo) {
        return rmaRequestRepository.findByRmaNo(rmaNo)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "RMA not found: " + rmaNo)))
                .flatMap(this::loadItemsAndToDTO);
    }

    public Flux<RmaDTO> listByCustomer(Long customerId, int page, int size) {
        return rmaRequestRepository.findByCustomerIdOrderByCreateTimeDesc(
                        customerId, org.springframework.data.domain.PageRequest.of(page, size))
                .flatMap(this::loadItemsAndToDTO);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private Mono<RmaRequest> findById(Long rmaId) {
        return rmaRequestRepository.findById(rmaId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "RMA not found: " + rmaId)));
    }

    private Mono<RmaDTO> loadItemsAndToDTO(RmaRequest rma) {
        return rmaItemRepository.findByRmaId(rma.getId())
                .collectList()
                .map(items -> {
                    rma.setItems(items);
                    return toDTO(rma);
                });
    }

    private Mono<RmaStatusHistory> recordHistory(Long rmaId, RmaStatus from, RmaStatus to,
                                                  Long operatorId, String operatorType,
                                                  String remark) {
        return historyRepository.save(RmaStatusHistory.builder()
                .rmaId(rmaId)
                .fromStatus(from)
                .toStatus(to)
                .operatorId(operatorId)
                .operatorType(operatorType)
                .remark(remark)
                .build());
    }

    private void validateReturnWindow(CreateRmaRequest req, RmaRegionConfig config) {
        // TODO: call order-service to get deliveredAt / shippedAt and compare against
        // config.getReturnWindowDays(). Skipping cross-service call until order client is wired.
    }

    private boolean shouldAutoApprove(CreateRmaRequest req, RmaRegionConfig config) {
        if (config.getAutoApproveThreshold() == null) return false;
        BigDecimal total = req.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.compareTo(config.getAutoApproveThreshold()) < 0;
    }

    private BigDecimal calculateRefundAmount(CreateRmaRequest req,
                                              RmaRegionConfig config,
                                              boolean orderShipped) {
        BigDecimal productAmount = req.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Tax refund only when goods have NOT shipped yet
        // TODO: add actual tax amount once tax-service client is wired
        return productAmount;
    }

    private BigDecimal calculateRefundAmountFromItems(List<RmaItem> items,
                                                       RmaRegionConfig config,
                                                       boolean orderShipped) {
        BigDecimal productAmount = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return productAmount;
    }

    private String generateRmaNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(java.time.ZoneOffset.UTC)
                .format(Instant.now());
        return "RMA" + ts + String.format("%06d", SEQ.incrementAndGet() % 1_000_000);
    }

    private RmaDTO toDTO(RmaRequest rma) {
        List<RmaItemDTO> itemDTOs = rma.getItems() == null ? List.of() :
                rma.getItems().stream()
                        .map(i -> RmaItemDTO.builder()
                                .id(i.getId())
                                .orderItemId(i.getOrderItemId())
                                .productId(i.getProductId())
                                .sku(i.getSku())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .reason(i.getReason())
                                .condition(i.getCondition())
                                .build())
                        .collect(Collectors.toList());

        return RmaDTO.builder()
                .id(rma.getId())
                .rmaNo(rma.getRmaNo())
                .tenantId(rma.getTenantId())
                .orderId(rma.getOrderId())
                .customerId(rma.getCustomerId())
                .type(rma.getType())
                .rmaStatus(rma.getRmaStatus())
                .regionCode(rma.getRegionCode())
                .reason(rma.getReason())
                .description(rma.getDescription())
                .refundAmount(rma.getRefundAmount())
                .refundCurrency(rma.getRefundCurrency())
                .taxRefundIncluded(rma.getTaxRefundIncluded())
                .trackingNo(rma.getTrackingNo())
                .carrier(rma.getCarrier())
                .merchantNotes(rma.getMerchantNotes())
                .approvedAt(rma.getApprovedAt())
                .receivedAt(rma.getReceivedAt())
                .completedAt(rma.getCompletedAt())
                .createdAt(rma.getCreatedAt())
                .updatedAt(rma.getUpdatedAt())
                .items(itemDTOs)
                .build();
    }
}
