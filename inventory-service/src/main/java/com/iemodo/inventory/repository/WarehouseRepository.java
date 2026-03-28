package com.iemodo.inventory.repository;

import com.iemodo.inventory.domain.Warehouse;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Warehouse} entity.
 */
@Repository
public interface WarehouseRepository extends ReactiveCrudRepository<Warehouse, Long> {

    Flux<Warehouse> findAllByIsActiveTrueOrderByWarehouseCode();

    Flux<Warehouse> findByCountryCodeAndIsActiveTrue(String countryCode);

    Mono<Warehouse> findByWarehouseCode(String warehouseCode);

    Mono<Boolean> existsByWarehouseCode(String warehouseCode);

    @Query("SELECT * FROM warehouses WHERE is_active = true AND is_default = true LIMIT 1")
    Mono<Warehouse> findDefaultWarehouse();

    @Query("SELECT * FROM warehouses WHERE is_active = true AND warehouse_type = :type")
    Flux<Warehouse> findByType(String type);
}
