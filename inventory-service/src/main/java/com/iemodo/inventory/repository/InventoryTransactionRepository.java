package com.iemodo.inventory.repository;

import com.iemodo.inventory.domain.InventoryTransaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for {@link InventoryTransaction} entity.
 */
@Repository
public interface InventoryTransactionRepository extends ReactiveCrudRepository<InventoryTransaction, Long> {

    Flux<InventoryTransaction> findBySkuIdOrderByCreatedAtDesc(Long skuId);

    Flux<InventoryTransaction> findByWarehouseIdAndSkuIdOrderByCreatedAtDesc(Long warehouseId, Long skuId);

    Flux<InventoryTransaction> findByReferenceNo(String referenceNo);
}
