package com.iemodo.product.repository;

import com.iemodo.product.domain.ProductCountryVisibility;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link ProductCountryVisibility} entity.
 */
@Repository
public interface ProductCountryVisibilityRepository extends ReactiveCrudRepository<ProductCountryVisibility, Long> {

    Flux<ProductCountryVisibility> findAllByProductId(Long productId);

    Mono<ProductCountryVisibility> findByProductIdAndCountryCode(Long productId, String countryCode);

    Flux<ProductCountryVisibility> findByCountryCodeAndIsVisibleTrue(String countryCode);

    Flux<ProductCountryVisibility> findByCountryCodeAndIsPurchasableTrue(String countryCode);

    @Query("SELECT EXISTS(SELECT 1 FROM product_country_visibility " +
           "WHERE product_id = :productId AND country_code = :countryCode AND is_visible = true)")
    Mono<Boolean> isVisibleInCountry(Long productId, String countryCode);

    @Query("SELECT EXISTS(SELECT 1 FROM product_country_visibility " +
           "WHERE product_id = :productId AND country_code = :countryCode AND is_purchasable = true)")
    Mono<Boolean> isPurchasableInCountry(Long productId, String countryCode);

    Mono<Void> deleteByProductId(Long productId);
}
