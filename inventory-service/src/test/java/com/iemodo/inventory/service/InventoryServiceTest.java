package com.iemodo.inventory.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.inventory.domain.Inventory;
import com.iemodo.inventory.domain.InventoryTransaction;
import com.iemodo.inventory.repository.InventoryRepository;
import com.iemodo.inventory.repository.InventoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("InventoryService")
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private InventoryCacheService cacheService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inventoryService = new InventoryService(inventoryRepository, transactionRepository, cacheService);
    }

    @Test
    @DisplayName("getTotalSellable: should return sum of sellable stock")
    void getTotalSellable_shouldReturnSum() {
        when(inventoryRepository.sumSellableBySkuId(1L)).thenReturn(Mono.just(150));

        StepVerifier.create(inventoryService.getTotalSellable(1L))
                .assertNext(total -> assertThat(total).isEqualTo(150))
                .verifyComplete();
    }

    @Test
    @DisplayName("getTotalSellable: should return 0 when no stock")
    void getTotalSellable_shouldReturnZero_whenNoStock() {
        when(inventoryRepository.sumSellableBySkuId(1L)).thenReturn(Mono.empty());

        StepVerifier.create(inventoryService.getTotalSellable(1L))
                .assertNext(total -> assertThat(total).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    @DisplayName("hasEnoughStock: should return true when sufficient")
    void hasEnoughStock_shouldReturnTrue_whenSufficient() {
        when(inventoryRepository.sumSellableBySkuId(1L)).thenReturn(Mono.just(100));

        StepVerifier.create(inventoryService.hasEnoughStock(1L, 50))
                .assertNext(result -> assertThat(result).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("hasEnoughStock: should return false when insufficient")
    void hasEnoughStock_shouldReturnFalse_whenInsufficient() {
        when(inventoryRepository.sumSellableBySkuId(1L)).thenReturn(Mono.just(30));

        StepVerifier.create(inventoryService.hasEnoughStock(1L, 50))
                .assertNext(result -> assertThat(result).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("getInventory: should return inventory when exists")
    void getInventory_shouldReturn_whenExists() {
        Inventory inv = Inventory.builder()
                .id(1L)
                .warehouseId(1L)
                .skuId(1L)
                .availableQty(100)
                .reservedQty(20)
                .build();

        when(inventoryRepository.findByWarehouseIdAndSkuId(1L, 1L)).thenReturn(Mono.just(inv));

        StepVerifier.create(inventoryService.getInventory(1L, 1L))
                .assertNext(result -> {
                    assertThat(result.getAvailable()).isEqualTo(100);
                    assertThat(result.getReserved()).isEqualTo(20);
                    assertThat(result.getSellable()).isEqualTo(80);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getInventory: should fail when not exists")
    void getInventory_shouldFail_whenNotExists() {
        when(inventoryRepository.findByWarehouseIdAndSkuId(1L, 1L)).thenReturn(Mono.empty());

        StepVerifier.create(inventoryService.getInventory(1L, 1L))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    @DisplayName("Inventory domain: hasEnoughStock should work correctly")
    void inventoryDomain_hasEnoughStock() {
        Inventory inv = Inventory.builder()
                .availableQty(100)
                .reservedQty(20)
                .build();

        assertThat(inv.hasEnoughStock(50)).isTrue();  // 100 - 20 = 80 available
        assertThat(inv.hasEnoughStock(80)).isTrue();
        assertThat(inv.hasEnoughStock(81)).isFalse();
    }

    @Test
    @DisplayName("Inventory domain: isLowStock should work correctly")
    void inventoryDomain_isLowStock() {
        Inventory inv = Inventory.builder()
                .availableQty(10)
                .reorderPoint(15)
                .build();

        assertThat(inv.isLowStock()).isTrue();

        inv.setAvailableQty(20);
        assertThat(inv.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("Inventory domain: stock operations should update quantities")
    void inventoryDomain_stockOperations() {
        Inventory inv = Inventory.builder()
                .availableQty(100)
                .reservedQty(0)
                .lockedQty(0)
                .build();

        // Reserve
        inv.reserve(10);
        assertThat(inv.getAvailable()).isEqualTo(100);
        assertThat(inv.getReserved()).isEqualTo(10);
        assertThat(inv.getSellable()).isEqualTo(90);

        // Confirm
        inv.confirm(10);
        assertThat(inv.getAvailable()).isEqualTo(90);
        assertThat(inv.getReserved()).isEqualTo(0);

        // Inbound
        inv.inbound(50);
        assertThat(inv.getAvailable()).isEqualTo(140);

        // Lock
        inv.lock(20);
        assertThat(inv.getAvailable()).isEqualTo(120);
        assertThat(inv.getLocked()).isEqualTo(20);
    }
}
