package com.iemodo.product.repository;

import com.iemodo.product.domain.Brand;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Brand} entity.
 */
@Repository
public interface BrandRepository extends ReactiveCrudRepository<Brand, Long> {

    Flux<Brand> findAllByIsActiveTrueOrderBySortOrderAsc();

    Mono<Brand> findByName(String name);

    Mono<Boolean> existsByName(String name);

    @Query("SELECT * FROM brands WHERE name ILIKE :pattern AND is_active = true")
    Flux<Brand> searchByName(String pattern);

    @Query("SELECT * FROM brands WHERE country_code = :countryCode AND is_active = true")
    Flux<Brand> findByCountryCode(String countryCode);
}
