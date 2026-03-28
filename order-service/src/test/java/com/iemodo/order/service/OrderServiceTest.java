package com.iemodo.order.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.exception.InsufficientStockException;
import com.iemodo.order.domain.Order;
import com.iemodo.order.domain.OrderItem;
import com.iemodo.order.domain.OrderStatus;
import com.iemodo.order.dto.CreateOrderRequest;
import com.iemodo.order.repository.OrderItemRepository;
import com.iemodo.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("OrderService")
class OrderServiceTest {

    @Mock private OrderRepository         orderRepository;
    @Mock private OrderItemRepository     orderItemRepository;
    @Mock private InventoryRedisService   inventoryRedisService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, orderItemRepository, inventoryRedisService);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private CreateOrderRequest buildRequest() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerId(1L);
        req.setCurrency("USD");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(100L);
        item.setSku("SKU-001");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("19.99"));
        req.setItems(List.of(item));
        return req;
    }

    private Order savedOrder() {
        return Order.builder()
                .id(1L)
                .orderNo("ORD20260321000001")
                .tenantId("tenant_001")
                .customerId(1L)
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("39.98"))
                .currency("USD")
                .createTime(Instant.now())
                .updateTime(Instant.now())
                .build();
    }

    private OrderItem savedItem() {
        return OrderItem.builder()
                .id(1L).orderId(1L)
                .productId(100L).sku("SKU-001")
                .quantity(2).unitPrice(new BigDecimal("19.99"))
                .subtotal(new BigDecimal("39.98"))
                .build();
    }

    // ─── Create order ──────────────────────────────────────────────────────

    @SuppressWarnings({ "null", "unchecked" })
@Test
    @DisplayName("createOrder: should succeed when Redis stock is available")
    void createOrder_shouldSucceed_whenStockAvailable() {
        when(inventoryRedisService.preDeduct(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder()));
        when(orderItemRepository.saveAll(any(Iterable.class))).thenReturn(Flux.just(savedItem()));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(savedItem()));

        StepVerifier.create(orderService.createOrder(buildRequest(), "tenant_001"))
                .assertNext(dto -> {
                    assertThat(dto.getOrderNo()).startsWith("ORD");
                    assertThat(dto.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
                    assertThat(dto.getTotalAmount()).isEqualByComparingTo("39.98");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createOrder: should fail with InsufficientStockException when Redis rejects")
    void createOrder_shouldFail_whenInsufficientStock() {
        when(inventoryRedisService.preDeduct(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.error(new InsufficientStockException("SKU-001")));
        // rollback is called in onErrorResume — must not return null
        when(inventoryRedisService.rollback(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrder(buildRequest(), "tenant_001"))
                .expectError(InsufficientStockException.class)
                .verify();
    }

    // ─── Get order ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrder: should return order with items when found")
    void getOrder_shouldReturnOrderWithItems_whenFound() {
        Order order = savedOrder();
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(savedItem()));

        StepVerifier.create(orderService.getOrder(1L))
                .assertNext(dto -> {
                    assertThat(dto.getId()).isEqualTo(1L);
                    assertThat(dto.getItems()).hasSize(1);
                    assertThat(dto.getItems().get(0).getSku()).isEqualTo("SKU-001");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getOrder: should fail with ORDER_NOT_FOUND when order does not exist")
    void getOrder_shouldFail_whenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(999L))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.ORDER_NOT_FOUND)
                .verify();
    }

    // ─── Cancel order ─────────────────────────────────────────────────────

    @SuppressWarnings("null")
@Test
    @DisplayName("cancelOrder: should cancel PENDING_PAYMENT order and rollback Redis")
    void cancelOrder_shouldCancel_whenPendingPayment() {
        Order order = savedOrder(); // status = PENDING_PAYMENT
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(savedItem()));
        when(inventoryRedisService.rollback(anyString(), anyString(), anyInt()))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.cancelOrder(1L, "tenant_001"))
                .assertNext(dto -> assertThat(dto.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED))
                .verifyComplete();
    }

    @Test
    @DisplayName("cancelOrder: should fail with INVALID_ORDER_STATUS when already cancelled")
    void cancelOrder_shouldFail_whenAlreadyCancelled() {
        Order cancelled = savedOrder();
        cancelled.setOrderStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Mono.just(cancelled));

        StepVerifier.create(orderService.cancelOrder(1L, "tenant_001"))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.INVALID_ORDER_STATUS)
                .verify();
    }
}
