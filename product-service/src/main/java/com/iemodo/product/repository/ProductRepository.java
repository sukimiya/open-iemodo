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

    @Query("SELECT * FROM products WHERE id = :id AND is_valid = true")
    Mono<Product> findByIdAndIsValid(Long id);

    Mono<Boolean> existsByProductCode(String productCode);

    @Query("SELECT * FROM products WHERE product_status = :productStatus AND is_valid = true ORDER BY create_time DESC")
    Flux<Product> findByProductStatusAndIsValidOrderByCreateTimeDesc(String productStatus);

    @Query("SELECT * FROM products WHERE category_id = :categoryId AND product_status = :productStatus AND is_valid = true")
    Flux<Product> findByCategoryIdAndProductStatusAndIsValid(Long categoryId, String productStatus);

    @Query("SELECT * FROM products WHERE brand_id = :brandId AND product_status = :productStatus AND is_valid = true")
    Flux<Product> findByBrandIdAndProductStatusAndIsValid(Long brandId, String productStatus);

    @Query("SELECT * FROM products WHERE is_featured = true AND product_status = :productStatus AND is_valid = true")
    Flux<Product> findByIsFeaturedTrueAndProductStatusAndIsValid(String productStatus);

    @Query("SELECT * FROM products WHERE is_new_arrival = true AND product_status = :productStatus AND is_valid = true")
    Flux<Product> findByIsNewArrivalTrueAndProductStatusAndIsValid(String productStatus);

    @Query("SELECT * FROM products WHERE product_status = 'ACTIVE' AND is_valid = true " +
           "AND (title ILIKE :keyword OR search_keywords ILIKE :keyword) " +
           "ORDER BY sale_count DESC LIMIT :limit OFFSET :offset")
    Flux<Product> searchByKeyword(String keyword, int limit, int offset);

    @Query("SELECT COUNT(*) FROM products WHERE product_status = 'ACTIVE' AND is_valid = true " +
           "AND (title ILIKE :keyword OR search_keywords ILIKE :keyword)")
    Mono<Long> countByKeyword(String keyword);

    @Query("UPDATE products SET view_count = view_count + 1 WHERE id = :id")
    Mono<Integer> incrementViewCount(Long id);

    @Query("UPDATE products SET sale_count = sale_count + :quantity WHERE id = :id")
    Mono<Integer> incrementSaleCount(Long id, int quantity);

}
