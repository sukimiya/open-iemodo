package com.iemodo.product.repository;

import com.iemodo.product.domain.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Product} entity.
 */
@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    Mono<Product> findByProductCode(String productCode);

    Mono<Product> findByIdAndIsValid(Long id);

    Mono<Boolean> existsByProductCode(String productCode);

    Flux<Product> findByProductStatusAndIsValidOrderByCreateTimeDesc(String productStatus);

    Flux<Product> findByCategoryIdAndProductStatusAndIsValid(Long categoryId, String productStatus);

    Flux<Product> findByBrandIdAndProductStatusAndIsValid(Long brandId, String productStatus);

    Flux<Product> findByIsFeaturedTrueAndProductStatusAndIsValid(String productStatus);

    Flux<Product> findByIsNewArrivalTrueAndProductStatusAndIsValid(String productStatus);

    @Query("SELECT * FROM products WHERE product_status = 'ACTIVE' AND is_valid = 1 " +
           "AND (title ILIKE :keyword OR search_keywords ILIKE :keyword) " +
           "ORDER BY sale_count DESC LIMIT :limit OFFSET :offset")
    Flux<Product> searchByKeyword(String keyword, int limit, int offset);

    @Query("SELECT COUNT(*) FROM products WHERE product_status = 'ACTIVE' AND is_valid = 1 " +
           "AND (title ILIKE :keyword OR search_keywords ILIKE :keyword)")
    Mono<Long> countByKeyword(String keyword);

    @Query("UPDATE products SET view_count = view_count + 1 WHERE id = :id")
    Mono<Integer> incrementViewCount(Long id);

    @Query("UPDATE products SET sale_count = sale_count + :quantity WHERE id = :id")
    Mono<Integer> incrementSaleCount(Long id, int quantity);
}
