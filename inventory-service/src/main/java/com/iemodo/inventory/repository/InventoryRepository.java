package com.iemodo.inventory.repository;

import com.iemodo.inventory.domain.Inventory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for {@link Inventory} entity.
 */
@Repository
public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Long> {

    Mono<Inventory> findByWarehouseIdAndSkuId(Long warehouseId, Long skuId);

    Flux<Inventory> findAllBySkuId(Long skuId);

    Flux<Inventory> findAllByWarehouseId(Long warehouseId);

    Flux<Inventory> findBySkuIdAndAvailableQtyGreaterThan(Long skuId, int minQty);

    @Query("SELECT * FROM inventory WHERE sku_id = :skuId AND available_qty - reserved_qty >= :quantity")
    Flux<Inventory> findBySkuIdWithSellableStock(Long skuId, int quantity);

    @Query("SELECT SUM(available_qty) FROM inventory WHERE sku_id = :skuId")
    Mono<Integer> sumAvailableBySkuId(Long skuId);

    @Query("SELECT SUM(available_qty - reserved_qty) FROM inventory WHERE sku_id = :skuId")
    Mono<Integer> sumSellableBySkuId(Long skuId);

    @Query("SELECT * FROM inventory WHERE reorder_point IS NOT NULL AND available_qty <= reorder_point")
    Flux<Inventory> findLowStockItems();

    @Query("UPDATE inventory SET available_qty = available_qty + :quantity, version = version + 1, " +
           "last_stock_in_at = NOW(), updated_at = NOW() " +
           "WHERE warehouse_id = :warehouseId AND sku_id = :skuId")
    Mono<Integer> increaseAvailable(Long warehouseId, Long skuId, int quantity);

    @Query("UPDATE inventory SET available_qty = available_qty - :quantity, version = version + 1, " +
           "last_stock_out_at = NOW(), updated_at = NOW() " +
           "WHERE warehouse_id = :warehouseId AND sku_id = :skuId AND available_qty >= :quantity")
    Mono<Integer> decreaseAvailable(Long warehouseId, Long skuId, int quantity);

    @Query("UPDATE inventory SET reserved_qty = reserved_qty + :quantity, version = version + 1, " +
           "updated_at = NOW() WHERE warehouse_id = :warehouseId AND sku_id = :skuId")
    Mono<Integer> increaseReserved(Long warehouseId, Long skuId, int quantity);

    @Query("UPDATE inventory SET reserved_qty = GREATEST(0, reserved_qty - :quantity), version = version + 1, " +
           "updated_at = NOW() WHERE warehouse_id = :warehouseId AND sku_id = :skuId")
    Mono<Integer> decreaseReserved(Long warehouseId, Long skuId, int quantity);
}
