package com.iemodo.inventory.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.inventory.domain.Inventory;
import com.iemodo.inventory.domain.InventoryTransaction;
import com.iemodo.inventory.repository.InventoryRepository;
import com.iemodo.inventory.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Inventory management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryCacheService cacheService;

    /**
     * Get inventory by warehouse and SKU.
     */
    public Mono<Inventory> getInventory(Long warehouseId, Long skuId) {
        return inventoryRepository.findByWarehouseIdAndSkuId(warehouseId, skuId)
                .switchIfEmpty(Mono.error(new BusinessException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Inventory not found")));
    }

    /**
     * Get total available stock across all warehouses for an SKU.
     */
    public Mono<Integer> getTotalAvailable(Long skuId) {
        return inventoryRepository.sumAvailableBySkuId(skuId)
                .defaultIfEmpty(0);
    }

    /**
     * Get total sellable stock across all warehouses for an SKU.
     */
    public Mono<Integer> getTotalSellable(Long skuId) {
        return inventoryRepository.sumSellableBySkuId(skuId)
                .defaultIfEmpty(0);
    }

    /**
     * Reserve stock for an order (pre-deduct).
     */
    @Transactional
    public Mono<Boolean> reserveStock(Long warehouseId, Long skuId, int quantity, String orderNo) {
        return inventoryRepository.findByWarehouseIdAndSkuId(warehouseId, skuId)
                .filter(inv -> inv.hasEnoughStock(quantity))
                .flatMap(inv -> {
                    // Update database
                    return inventoryRepository.increaseReserved(warehouseId, skuId, quantity)
                            .filter(updated -> updated > 0)
                            .flatMap(updated -> {
                                // Record transaction
                                InventoryTransaction tx = InventoryTransaction.builder()
                                        .warehouseId(warehouseId)
                                        .skuId(skuId)
                                        .transactionType(InventoryTransaction.TransactionType.RESERVED.name())
                                        .quantity(-quantity)
                                        .beforeAvailable(inv.getAvailable())
                                        .afterAvailable(inv.getAvailable())
                                        .beforeReserved(inv.getReserved())
                                        .afterReserved(inv.getReserved() + quantity)
                                        .referenceNo(orderNo)
                                        .referenceType(InventoryTransaction.ReferenceType.ORDER.name())
                                        .reason("Order reservation")
                                        .build();
                                return transactionRepository.save(tx);
                            })
                            .map(tx -> true);
                })
                .defaultIfEmpty(false)
                .doOnSuccess(result -> {
                    if (result) {
                        log.info("Reserved {} stock for SKU {} in warehouse {}, order {}", 
                                quantity, skuId, warehouseId, orderNo);
                    }
                });
    }

    /**
     * Release reserved stock (order cancelled).
     */
    @Transactional
    public Mono<Boolean> releaseStock(Long warehouseId, Long skuId, int quantity, String orderNo) {
        return inventoryRepository.findByWarehouseIdAndSkuId(warehouseId, skuId)
                .flatMap(inv -> {
                    return inventoryRepository.decreaseReserved(warehouseId, skuId, quantity)
                            .filter(updated -> updated > 0)
                            .flatMap(updated -> {
                                InventoryTransaction tx = InventoryTransaction.builder()
                                        .warehouseId(warehouseId)
                                        .skuId(skuId)
                                        .transactionType(InventoryTransaction.TransactionType.RELEASED.name())
                                        .quantity(quantity)
                                        .beforeAvailable(inv.getAvailable())
                                        .afterAvailable(inv.getAvailable())
                                        .beforeReserved(inv.getReserved())
                                        .afterReserved(Math.max(0, inv.getReserved() - quantity))
                                        .referenceNo(orderNo)
                                        .referenceType(InventoryTransaction.ReferenceType.ORDER.name())
                                        .reason("Order cancellation")
                                        .build();
                                return transactionRepository.save(tx);
                            })
                            .map(tx -> true);
                })
                .defaultIfEmpty(false)
                .doOnSuccess(result -> {
                    if (result) {
                        log.info("Released {} stock for SKU {} in warehouse {}, order {}", 
                                quantity, skuId, warehouseId, orderNo);
                    }
                });
    }

    /**
     * Confirm stock deduction (order shipped).
     */
    @Transactional
    public Mono<Boolean> confirmStock(Long warehouseId, Long skuId, int quantity, String orderNo) {
        return inventoryRepository.findByWarehouseIdAndSkuId(warehouseId, skuId)
                .flatMap(inv -> {
                    // Decrease available and reserved
                    return inventoryRepository.decreaseAvailable(warehouseId, skuId, quantity)
                            .filter(updated -> updated > 0)
                            .flatMap(updated -> inventoryRepository.decreaseReserved(warehouseId, skuId, quantity))
                            .filter(updated -> updated > 0)
                            .flatMap(updated -> {
                                InventoryTransaction tx = InventoryTransaction.builder()
                                        .warehouseId(warehouseId)
                                        .skuId(skuId)
                                        .transactionType(InventoryTransaction.TransactionType.OUTBOUND.name())
                                        .quantity(-quantity)
                                        .beforeAvailable(inv.getAvailable())
                                        .afterAvailable(inv.getAvailable() - quantity)
                                        .beforeReserved(inv.getReserved())
                                        .afterReserved(inv.getReserved() - quantity)
                                        .referenceNo(orderNo)
                                        .referenceType(InventoryTransaction.ReferenceType.ORDER.name())
                                        .reason("Order shipment")
                                        .build();
                                return transactionRepository.save(tx);
                            })
                            .map(tx -> true);
                })
                .defaultIfEmpty(false);
    }

    /**
     * Inbound stock.
     */
    @Transactional
    public Mono<Inventory> inbound(Long warehouseId, Long skuId, int quantity, String referenceNo, String reason) {
        return inventoryRepository.findByWarehouseIdAndSkuId(warehouseId, skuId)
                .switchIfEmpty(createNewInventory(warehouseId, skuId))
                .flatMap(inv -> {
                    return inventoryRepository.increaseAvailable(warehouseId, skuId, quantity)
                            .filter(updated -> updated > 0)
                            .flatMap(updated -> {
                                InventoryTransaction tx = InventoryTransaction.builder()
                                        .warehouseId(warehouseId)
                                        .skuId(skuId)
                                        .transactionType(InventoryTransaction.TransactionType.INBOUND.name())
                                        .quantity(quantity)
                                        .beforeAvailable(inv.getAvailable())
                                        .afterAvailable(inv.getAvailable() + quantity)
                                        .beforeReserved(inv.getReserved())
                                        .afterReserved(inv.getReserved())
                                        .referenceNo(referenceNo)
                                        .referenceType(InventoryTransaction.ReferenceType.ORDER.name())
                                        .reason(reason != null ? reason : "Stock inbound")
                                        .build();
                                return transactionRepository.save(tx)
                                        .then(getInventory(warehouseId, skuId));
                            });
                })
                .doOnSuccess(inv -> log.info("Inbound {} stock for SKU {} in warehouse {}", 
                        quantity, skuId, warehouseId));
    }

    /**
     * Adjust stock (manual correction).
     */
    @Transactional
    public Mono<Inventory> adjust(Long warehouseId, Long skuId, int newQuantity, String reason) {
        return inventoryRepository.findByWarehouseIdAndSkuId(warehouseId, skuId)
                .switchIfEmpty(createNewInventory(warehouseId, skuId))
                .flatMap(inv -> {
                    int delta = newQuantity - inv.getTotal();
                    
                    return inventoryRepository.increaseAvailable(warehouseId, skuId, delta)
                            .filter(updated -> updated > 0)
                            .flatMap(updated -> {
                                InventoryTransaction tx = InventoryTransaction.builder()
                                        .warehouseId(warehouseId)
                                        .skuId(skuId)
                                        .transactionType(InventoryTransaction.TransactionType.ADJUST.name())
                                        .quantity(delta)
                                        .beforeAvailable(inv.getAvailable())
                                        .afterAvailable(newQuantity)
                                        .beforeReserved(inv.getReserved())
                                        .afterReserved(inv.getReserved())
                                        .reason(reason)
                                        .build();
                                return transactionRepository.save(tx)
                                        .then(getInventory(warehouseId, skuId));
                            });
                });
    }

    /**
     * Get low stock items.
     */
    public Flux<Inventory> getLowStockItems() {
        return inventoryRepository.findLowStockItems();
    }

    /**
     * Get inventory transactions for an SKU.
     */
    public Flux<InventoryTransaction> getTransactions(Long skuId) {
        return transactionRepository.findBySkuIdOrderByCreatedAtDesc(skuId);
    }

    // ─── Helper methods ────────────────────────────────────────────────────

    private Mono<Inventory> createNewInventory(Long warehouseId, Long skuId) {
        Inventory inv = Inventory.builder()
                .warehouseId(warehouseId)
                .skuId(skuId)
                .availableQty(0)
                .reservedQty(0)
                .lockedQty(0)
                .inboundQty(0)
                .version(0)
                .build();
        return inventoryRepository.save(inv);
    }

    public Mono<Boolean> hasEnoughStock(Long skuId, int quantity) {
        return getTotalSellable(skuId)
                .map(total -> total >= quantity);
    }

    public Mono<List<Inventory>> findWarehousesWithStock(Long skuId, int quantity) {
        return inventoryRepository.findBySkuIdWithSellableStock(skuId, quantity)
                .collectList();
    }
}
