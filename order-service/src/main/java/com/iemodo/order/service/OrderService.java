package com.iemodo.order.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.order.domain.Order;
import com.iemodo.order.domain.OrderItem;
import com.iemodo.order.domain.OrderStatus;
import com.iemodo.order.dto.CreateOrderRequest;
import com.iemodo.order.dto.OrderDTO;
import com.iemodo.order.dto.OrderItemDTO;
import com.iemodo.order.repository.OrderItemRepository;
import com.iemodo.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

/**
 * Core order business logic — fully reactive (Mono/Flux throughout).
 *
 * <p>Order creation flow:
 * <ol>
 *   <li>Redis Lua pre-deduct inventory for each SKU (atomic, fail-fast).</li>
 *   <li>Persist {@link Order} row.</li>
 *   <li>Persist {@link OrderItem} rows in batch.</li>
 *   <li>Return assembled {@link OrderDTO}.</li>
 * </ol>
 *
 * <p>On any error after step 1, the Redis pre-deduction is rolled back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository         orderRepository;
    private final OrderItemRepository     orderItemRepository;
    private final InventoryRedisService   inventoryRedisService;

    /** Simple in-process sequence for order number uniqueness (prod: use Snowflake). */
    private static final AtomicLong SEQ = new AtomicLong(0);

    // ─── Create ───────────────────────────────────────────────────────────

    @Transactional
    public Mono<OrderDTO> createOrder(CreateOrderRequest req, String tenantId) {
        // 1. Pre-deduct each SKU in Redis (sequential to avoid partial success)
        return preDeductAll(req, tenantId)
                // 2. Build and save Order
                .then(Mono.defer(() -> {
                    Order order = buildOrder(req, tenantId);
                    return orderRepository.save(order);
                }))
                // 3. Save OrderItems
                .flatMap(savedOrder -> {
                    List<OrderItem> items = buildItems(req, savedOrder.getId());
                    return orderItemRepository.saveAll(items)
                            .collectList()
                            .map(savedItems -> {
                                savedOrder.setItems(savedItems);
                                return savedOrder;
                            });
                })
                // 4. Convert to DTO
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Created order={} tenant={}", dto.getOrderNo(), tenantId))
                // 5. Rollback Redis on any error
                .onErrorResume(ex -> rollbackAll(req, tenantId).then(Mono.error(ex)));
    }

    // ─── Get ──────────────────────────────────────────────────────────────

    public Mono<OrderDTO> getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(order -> orderItemRepository.findByOrderId(order.getId())
                        .collectList()
                        .map(items -> {
                            order.setItems(items);
                            return toDTO(order);
                        }));
    }

    // ─── List (paginated) ─────────────────────────────────────────────────

    public Flux<OrderDTO> listOrders(Long customerId, int page, int size) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(
                        customerId, PageRequest.of(page, size))
                .flatMap(order -> orderItemRepository.findByOrderId(order.getId())
                        .collectList()
                        .map(items -> {
                            order.setItems(items);
                            return toDTO(order);
                        }));
    }

    // ─── Cancel ──────────────────────────────────────────────────────────

    @Transactional
    public Mono<OrderDTO> cancelOrder(Long orderId, String tenantId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(order -> {
                    order.transitionTo(OrderStatus.CANCELLED); // throws if not allowed
                    return orderRepository.save(order)
                            .flatMap(saved ->
                                    // Roll back Redis inventory for each item
                                    orderItemRepository.findByOrderId(saved.getId())
                                            .flatMap(item -> inventoryRedisService.rollback(
                                                    tenantId, item.getSku(), item.getQuantity()))
                                            .then(Mono.just(saved))
                            );
                })
                .flatMap(order -> orderItemRepository.findByOrderId(order.getId())
                        .collectList()
                        .map(items -> {
                            order.setItems(items);
                            return toDTO(order);
                        }))
                .doOnSuccess(dto -> log.info("Cancelled order={} tenant={}", dto.getOrderNo(), tenantId));
    }

    // ─── Status transition (internal — called by payment service callback) ─

    @Transactional
    public Mono<OrderDTO> transitionStatus(Long orderId, OrderStatus next) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(order -> {
                    order.transitionTo(next);
                    return orderRepository.save(order);
                })
                .flatMap(order -> orderItemRepository.findByOrderId(order.getId())
                        .collectList()
                        .map(items -> {
                            order.setItems(items);
                            return toDTO(order);
                        }))
                .doOnSuccess(dto -> log.info("Order={} transitioned to {}", dto.getOrderNo(), next));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private Mono<Void> preDeductAll(CreateOrderRequest req, String tenantId) {
        return Flux.fromIterable(req.getItems())
                .concatMap(item -> inventoryRedisService.preDeduct(
                        tenantId, item.getSku(), item.getQuantity()))
                .then();
    }

    private Mono<Void> rollbackAll(CreateOrderRequest req, String tenantId) {
        return Flux.fromIterable(req.getItems())
                .concatMap(item -> inventoryRedisService.rollback(
                        tenantId, item.getSku(), item.getQuantity()))
                .then();
    }

    private Order buildOrder(CreateOrderRequest req, String tenantId) {
        BigDecimal total = req.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Order.builder()
                .orderNo(generateOrderNo())
                .tenantId(tenantId)
                .customerId(req.getCustomerId())
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .totalAmount(total)
                .currency(req.getCurrency())
                .shippingName(req.getShippingName())
                .shippingPhone(req.getShippingPhone())
                .shippingAddr(req.getShippingAddr())
                .build();
    }

    private List<OrderItem> buildItems(CreateOrderRequest req, Long orderId) {
        return req.getItems().stream()
                .map(i -> OrderItem.builder()
                        .orderId(orderId)
                        .productId(i.getProductId())
                        .sku(i.getSku())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .collect(Collectors.toList());
    }

    private String generateOrderNo() {
        // Format: yyyyMMddHHmmss + 6-digit sequence
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(java.time.ZoneOffset.UTC)
                .format(Instant.now());
        return "ORD" + ts + String.format("%06d", SEQ.incrementAndGet() % 1_000_000);
    }

    private OrderDTO toDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems() == null ? List.of() :
                order.getItems().stream()
                        .map(i -> OrderItemDTO.builder()
                                .id(i.getId())
                                .productId(i.getProductId())
                                .sku(i.getSku())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .subtotal(i.getSubtotal())
                                .build())
                        .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .tenantId(order.getTenantId())
                .customerId(order.getCustomerId())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddr(order.getShippingAddr())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemDTOs)
                .build();
    }
}
