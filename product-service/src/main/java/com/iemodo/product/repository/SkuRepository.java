package com.iemodo.product.repository;

import com.iemodo.product.domain.Sku;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Sku} entity.
 */
@Repository
public interface SkuRepository extends ReactiveCrudRepository<Sku, Long> {

    Flux<Sku> findAllByProductIdAndIsValid(Long productId);

    Mono<Sku> findBySkuCode(String skuCode);

    Mono<Sku> findByIdAndIsValid(Long id);

    Mono<Boolean> existsBySkuCode(String skuCode);

    Mono<Boolean> existsByProductIdAndAttributeHashAndIsValid(Long productId, String attributeHash);

    @Query("SELECT * FROM skus WHERE product_id = :productId AND sku_status = 'ACTIVE' " +
           "AND stock_quantity > 0 AND is_valid = 1")
    Flux<Sku> findAvailableByProductId(Long productId);

    @Query("UPDATE skus SET stock_quantity = stock_quantity - :quantity, " +
           "reserved_quantity = reserved_quantity - :quantity WHERE id = :id " +
           "AND stock_quantity >= :quantity")
    Mono<Integer> deductStock(Long id, int quantity);

    @Query("UPDATE skus SET reserved_quantity = reserved_quantity + :quantity WHERE id = :id")
    Mono<Integer> reserveStock(Long id, int quantity);

    @Query("UPDATE skus SET reserved_quantity = GREATEST(0, reserved_quantity - :quantity) WHERE id = :id")
    Mono<Integer> releaseStock(Long id, int quantity);

    Object findByIdAndDeletedAtIsNull(long l);
}
