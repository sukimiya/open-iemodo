package com.iemodo.order.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.order.domain.DelayTaskStatus;
import com.iemodo.order.domain.Order;
import com.iemodo.order.domain.OrderDelayTask;
import com.iemodo.order.domain.OrderItem;
import com.iemodo.order.domain.OrderStatus;
import com.iemodo.order.dto.CreateOrderRequest;
import com.iemodo.order.dto.OrderDTO;
import com.iemodo.order.dto.OrderItemDTO;
import com.iemodo.order.dto.OrderTokenResponse;
import com.iemodo.order.repository.OrderDelayTaskRepository;
import com.iemodo.order.repository.OrderItemRepository;
import com.iemodo.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    private final OrderRepository            orderRepository;
    private final OrderItemRepository        orderItemRepository;
    private final InventoryRedisService      inventoryRedisService;
    private final OrderDelayTaskRepository   orderDelayTaskRepository;

    /** Simple in-process sequence for order number uniqueness (prod: use Snowflake). */
    private static final AtomicLong SEQ = new AtomicLong(0);

    /** Orders unpaid after this many minutes are auto-cancelled. */
    private static final long PAYMENT_TIMEOUT_MINUTES = 30;

    // ─── Create ───────────────────────────────────────────────────────────

    @Transactional
    public Mono<OrderDTO> createOrder(CreateOrderRequest req, String tenantId) {
        // 0. Idempotency check — if a token was provided, validate it first
        Mono<Void> idempotencyGuard = validateIdempotencyToken(req, tenantId);

        // 1. Pre-deduct each SKU in Redis (sequential to avoid partial success)
        return idempotencyGuard
                .then(preDeductAll(req, tenantId))
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
                // 4. Mark idempotency token as SUCCESS
                .flatMap(order -> {
                    if (req.getIdempotencyKey() == null) return Mono.just(order);
                    return inventoryRedisService.markSuccess(tenantId, req.getIdempotencyKey())
                            .thenReturn(order);
                })
                // 5. Schedule payment timeout task
                .flatMap(order -> orderDelayTaskRepository.save(buildDelayTask(order)).thenReturn(order))
                // 6. Convert to DTO
                .map(this::toDTO)
                .doOnSuccess(dto -> log.info("Created order={} tenant={}", dto.getOrderNo(), tenantId))
                // On error: rollback Redis inventory (idempotency token stays PENDING until TTL expires)
                .onErrorResume(ex -> rollbackAll(req, tenantId).then(Mono.error(ex)));
    }

    /** Issue a pre-registered idempotency token (orderNo) for the client to use at order creation. */
    public Mono<OrderTokenResponse> generateOrderToken(String tenantId) {
        String orderNo = generateOrderNo();
        return inventoryRedisService.registerToken(tenantId, orderNo)
                .thenReturn(new OrderTokenResponse(orderNo));
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

    // ─── Timeout cancel (called by scheduler) ────────────────────────────

    /**
     * Attempts to cancel an order whose payment window has expired.
     *
     * <p>Uses a two-step concurrency guard:
     * <ol>
     *   <li>Atomically claim the task row (PENDING → PROCESSING). If another instance
     *       already claimed it, returns empty.</li>
     *   <li>Cancel the order. {@link OptimisticLockingFailureException} on the Order row
     *       is swallowed — it means the order was concurrently modified and is no longer
     *       in a cancellable state.</li>
     * </ol>
     */
    public Mono<Void> cancelTimedOutOrder(OrderDelayTask task) {
        return orderDelayTaskRepository.claimTask(task.getId())
                .flatMap(claimed -> {
                    if (claimed == 0) return Mono.empty(); // another instance got here first

                    return orderRepository.findById(task.getOrderId())
                            .flatMap(order -> {
                                if (!order.isPendingPayment()) {
                                    // already paid or cancelled — nothing to do
                                    return orderDelayTaskRepository
                                            .updateStatus(task.getId(), DelayTaskStatus.SKIPPED.name())
                                            .then();
                                }
                                order.transitionTo(OrderStatus.CANCELLED);
                                return orderRepository.save(order)
                                        .flatMap(saved ->
                                                orderItemRepository.findByOrderId(saved.getId())
                                                        .flatMap(item -> inventoryRedisService.rollback(
                                                                task.getTenantId(), item.getSku(), item.getQuantity()))
                                                        .then()
                                        )
                                        .then(orderDelayTaskRepository
                                                .updateStatus(task.getId(), DelayTaskStatus.DONE.name())
                                                .then())
                                        .doOnSuccess(v -> log.info("Timed out order={} tenant={}",
                                                task.getOrderId(), task.getTenantId()));
                            })
                            .onErrorResume(OptimisticLockingFailureException.class, ex -> {
                                log.warn("Optimistic lock conflict cancelling order={}, skipping", task.getOrderId());
                                return Mono.empty();
                            });
                });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /**
     * Validates the idempotency token from the request.
     * <ul>
     *   <li>PENDING  → ok, proceed with order creation</li>
     *   <li>SUCCESS  → duplicate request; fetch and return the existing order</li>
     *   <li>NOT_FOUND → token expired or never issued; reject with error</li>
     * </ul>
     */
    private Mono<Void> validateIdempotencyToken(CreateOrderRequest req, String tenantId) {
        if (req.getIdempotencyKey() == null) return Mono.empty();

        return inventoryRedisService.checkIdempotency(tenantId, req.getIdempotencyKey())
                .flatMap(state -> {
                    if (state == 1L) return Mono.empty(); // PENDING — proceed
                    if (state == 2L) {                    // SUCCESS — duplicate
                        log.info("Duplicate order request, idempotencyKey={}", req.getIdempotencyKey());
                        return Mono.error(new BusinessException(
                                ErrorCode.DUPLICATE_ORDER, HttpStatus.CONFLICT,
                                "Order already created for idempotency key: " + req.getIdempotencyKey()));
                    }
                    // NOT_FOUND — token expired or invalid
                    return Mono.error(new BusinessException(
                            ErrorCode.INVALID_IDEMPOTENCY_TOKEN, HttpStatus.UNPROCESSABLE_ENTITY,
                            "Idempotency token not found or expired: " + req.getIdempotencyKey()));
                });
    }

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

        String orderNo = req.getIdempotencyKey() != null ? req.getIdempotencyKey() : generateOrderNo();
        return Order.builder()
                .orderNo(orderNo)
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

    private OrderDelayTask buildDelayTask(Order order) {
        return OrderDelayTask.builder()
                .orderId(order.getId())
                .tenantId(order.getTenantId())
                .taskType("PAYMENT_TIMEOUT")
                .executeTime(Instant.now().plus(PAYMENT_TIMEOUT_MINUTES, ChronoUnit.MINUTES))
                .taskStatus(DelayTaskStatus.PENDING)
                .build();
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
