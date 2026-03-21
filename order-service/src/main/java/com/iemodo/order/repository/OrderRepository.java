package com.iemodo.order.repository;

import com.iemodo.order.domain.Order;
import com.iemodo.order.domain.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {

    Mono<Order> findByOrderNo(String orderNo);

    Flux<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Mono<Long> countByCustomerId(Long customerId);

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY created_at DESC LIMIT :limit")
    Flux<Order> findRecentByStatus(OrderStatus status, int limit);
}
