package com.iemodo.inventory.repository;

import com.iemodo.inventory.domain.InventoryTransaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for {@link InventoryTransaction} entity.
 */
@Repository
public interface InventoryTransactionRepository extends ReactiveCrudRepository<InventoryTransaction, Long> {

    @Query("SELECT * FROM inventory_transactions WHERE sku_id = :skuId ORDER BY create_time DESC")
    Flux<InventoryTransaction> findBySkuIdOrderByCreatedAtDesc(Long skuId);

    @Query("SELECT * FROM inventory_transactions WHERE warehouse_id = :warehouseId AND sku_id = :skuId ORDER BY create_time DESC")
    Flux<InventoryTransaction> findByWarehouseIdAndSkuIdOrderByCreatedAtDesc(Long warehouseId, Long skuId);

    Flux<InventoryTransaction> findByReferenceNo(String referenceNo);
}
