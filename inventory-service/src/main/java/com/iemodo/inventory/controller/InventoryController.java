package com.iemodo.inventory.controller;

import com.iemodo.common.response.Response;
import com.iemodo.inventory.domain.Inventory;
import com.iemodo.inventory.domain.Warehouse;
import com.iemodo.inventory.service.InventoryCacheService;
import com.iemodo.inventory.service.InventoryService;
import com.iemodo.inventory.service.WarehouseAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for inventory management.
 * 
 * <p>Base path: /inv/api/v1
 */
@Slf4j
@RestController
@RequestMapping("/inv/api/v1")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryCacheService cacheService;
    private final WarehouseAllocationService allocationService;

    // ─── Inventory Queries ─────────────────────────────────────────────────

    @GetMapping("/inventory/{skuId}")
    public Mono<Response<Integer>> getTotalSellable(@PathVariable Long skuId) {
        return inventoryService.getTotalSellable(skuId)
                .map(Response::success);
    }

    @GetMapping("/warehouses/{warehouseId}/inventory/{skuId}")
    public Mono<Response<Inventory>> getWarehouseInventory(
            @PathVariable Long warehouseId,
            @PathVariable Long skuId) {
        return inventoryService.getInventory(warehouseId, skuId)
                .map(Response::success);
    }

    @GetMapping("/inventory/{skuId}/warehouses")
    public Mono<Response<java.util.List<Inventory>>> findWarehousesWithStock(
            @PathVariable Long skuId,
            @RequestParam(defaultValue = "1") int quantity) {
        return inventoryService.findWarehousesWithStock(skuId, quantity)
                .map(Response::success);
    }

    @GetMapping("/inventory/low-stock")
    public Flux<Response<Inventory>> getLowStockItems() {
        return inventoryService.getLowStockItems()
                .map(Response::success);
    }

    // ─── Stock Operations ──────────────────────────────────────────────────

    @PostMapping("/inventory/reserve")
    public Mono<Response<Boolean>> reserveStock(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam Long warehouseId,
            @RequestParam Long skuId,
            @RequestParam int quantity,
            @RequestParam String orderNo) {
        return inventoryService.reserveStock(tenantId, warehouseId, skuId, quantity, orderNo)
                .map(Response::success);
    }

    @PostMapping("/inventory/release")
    public Mono<Response<Boolean>> releaseStock(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam Long warehouseId,
            @RequestParam Long skuId,
            @RequestParam int quantity,
            @RequestParam String orderNo) {
        return inventoryService.releaseStock(tenantId, warehouseId, skuId, quantity, orderNo)
                .map(Response::success);
    }

    @PostMapping("/inventory/confirm")
    public Mono<Response<Boolean>> confirmStock(
            @RequestParam Long warehouseId,
            @RequestParam Long skuId,
            @RequestParam int quantity,
            @RequestParam String orderNo) {
        return inventoryService.confirmStock(warehouseId, skuId, quantity, orderNo)
                .map(Response::success);
    }

    @PostMapping("/inventory/inbound")
    public Mono<Response<Inventory>> inbound(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam Long warehouseId,
            @RequestParam Long skuId,
            @RequestParam int quantity,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) String reason) {
        return inventoryService.inbound(tenantId, warehouseId, skuId, quantity, referenceNo, reason)
                .map(Response::success);
    }

    // ─── Cache Operations (Anti-overselling) ────────────────────────────────

    @PostMapping("/cache/pre-deduct")
    public Mono<Response<Boolean>> preDeduct(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam Long skuId,
            @RequestParam int quantity) {
        return cacheService.preDeduct(tenantId, skuId, quantity)
                .map(Response::success);
    }

    @PostMapping("/cache/release")
    public Mono<Response<Boolean>> releaseCacheStock(
            @RequestHeader("X-TenantID") String tenantId,
            @RequestParam Long skuId,
            @RequestParam int quantity) {
        return cacheService.releaseStock(tenantId, skuId, quantity)
                .map(Response::success);
    }

    @GetMapping("/cache/stock/{skuId}")
    public Mono<Response<Integer>> getCacheStock(
            @RequestHeader("X-TenantID") String tenantId,
            @PathVariable Long skuId) {
        return cacheService.getStock(tenantId, skuId)
                .map(Response::success);
    }

    // ─── Warehouse Allocation ──────────────────────────────────────────────

    @PostMapping("/warehouses/allocate")
    public Mono<Response<WarehouseAllocationService.WarehouseAllocation>> allocateWarehouse(
            @RequestParam Long skuId,
            @RequestParam int quantity,
            @RequestParam String countryCode,
            @RequestParam(required = false) BigDecimal destLat,
            @RequestParam(required = false) BigDecimal destLon) {
        
        return allocationService.allocateWarehouse(skuId, quantity, countryCode, destLat, destLon,
                        WarehouseAllocationService.AllocationPreference.BALANCED)
                .map(Response::success);
    }

    @GetMapping("/warehouses/rank")
    public Mono<Response<java.util.List<WarehouseAllocationService.WarehouseAllocation>>> rankWarehouses(
            @RequestParam Long skuId,
            @RequestParam int quantity,
            @RequestParam String countryCode,
            @RequestParam(required = false) BigDecimal destLat,
            @RequestParam(required = false) BigDecimal destLon) {
        
        return allocationService.rankWarehouses(skuId, quantity, countryCode, destLat, destLon)
                .map(Response::success);
    }

    // ─── Transactions ──────────────────────────────────────────────────────

    @GetMapping("/inventory/{skuId}/transactions")
    public Flux<Response<com.iemodo.inventory.domain.InventoryTransaction>> getTransactions(
            @PathVariable Long skuId) {
        return inventoryService.getTransactions(skuId)
                .map(Response::success);
    }
}
