package com.iemodo.inventory.service;

import com.iemodo.inventory.domain.Inventory;
import com.iemodo.inventory.domain.Warehouse;
import com.iemodo.inventory.repository.InventoryRepository;
import com.iemodo.inventory.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Smart warehouse allocation service.
 * 
 * <p>Allocates warehouses based on:
 * <ul>
 *   <li>Stock availability
 *   <li>Distance to destination
 *   <li>Service level
 *   <li>Processing capacity
 *   <li>Cost efficiency
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseAllocationService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Allocation preference.
     */
    public enum AllocationPreference {
        FASTEST,      // Prioritize speed
        CHEAPEST,     // Prioritize cost
        BALANCED      // Balance speed and cost
    }

    /**
     * Allocate best warehouse for an order.
     *
     * @param skuId           SKU ID
     * @param quantity        Required quantity
     * @param countryCode     Destination country
     * @param preference      Allocation preference
     * @return Best warehouse allocation
     */
    public Mono<WarehouseAllocation> allocateWarehouse(
            Long skuId, 
            int quantity, 
            String countryCode,
            BigDecimal destLat, 
            BigDecimal destLon,
            AllocationPreference preference) {
        
        return findCandidateWarehouses(skuId, quantity, countryCode)
                .collectList()
                .flatMap(candidates -> {
                    if (candidates.isEmpty()) {
                        return Mono.error(new IllegalStateException("No warehouse with sufficient stock"));
                    }
                    
                    // Score and rank warehouses
                    List<WarehouseAllocation> allocations = candidates.stream()
                            .map(candidate -> scoreWarehouse(candidate, destLat, destLon, preference))
                            .sorted(Comparator.comparing(WarehouseAllocation::score).reversed())
                            .toList();
                    
                    return Mono.just(allocations.get(0));
                });
    }

    /**
     * Rank all warehouses for an order.
     */
    public Mono<List<WarehouseAllocation>> rankWarehouses(
            Long skuId, 
            int quantity,
            String countryCode,
            BigDecimal destLat, 
            BigDecimal destLon) {
        
        return findCandidateWarehouses(skuId, quantity, countryCode)
                .map(candidate -> scoreWarehouse(candidate, destLat, destLon, AllocationPreference.BALANCED))
                .sort(Comparator.comparing(WarehouseAllocation::score).reversed())
                .collectList();
    }

    /**
     * Multi-item allocation with splitting.
     */
    public Mono<Map<Long, Long>> allocateMultipleItems(
            Map<Long, Integer> items,  // SKU ID -> Quantity
            String countryCode,
            BigDecimal destLat,
            BigDecimal destLon) {
        
        // For simplicity, allocate all from the best warehouse for each item
        return Flux.fromIterable(items.entrySet())
                .concatMap(entry -> 
                        allocateWarehouse(entry.getKey(), entry.getValue(), countryCode, destLat, destLon, AllocationPreference.BALANCED)
                                .map(alloc -> Map.entry(entry.getKey(), alloc.warehouse().getId()))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    // ─── Helper methods ────────────────────────────────────────────────────

    private Flux<Inventory> findCandidateWarehouses(Long skuId, int quantity, String countryCode) {
        return inventoryRepository.findBySkuIdWithSellableStock(skuId, quantity)
                .flatMap(inv -> 
                        warehouseRepository.findById(inv.getWarehouseId())
                                .filter(w -> w.isActive() && w.hasCapacity())
                                .filter(w -> countryCode == null || w.getCountryCode().equals(countryCode))
                                .map(w -> inv)
                );
    }

    private WarehouseAllocation scoreWarehouse(
            Inventory inventory, 
            BigDecimal destLat, 
            BigDecimal destLon,
            AllocationPreference preference) {
        
        // This would be enhanced with actual warehouse data
        double score = 0.0;
        
        // Stock availability score (0-40)
        score += Math.min(40, inventory.getSellable() / 10.0);
        
        // Distance score (0-30) - closer is better
        // For now, use mock distance calculation
        double distanceScore = 30.0;
        score += distanceScore;
        
        // Service level score (0-20)
        score += 15.0; // Default service score
        
        // Capacity score (0-10)
        score += 8.0; // Default capacity score
        
        // Adjust based on preference
        switch (preference) {
            case FASTEST -> score *= 1.2;  // Boost distance factor
            case CHEAPEST -> score *= 0.9; // Reduce cost
            case BALANCED -> { /* No adjustment */ }
        }
        
        return new WarehouseAllocation(
                Warehouse.builder().id(inventory.getWarehouseId()).build(), // Placeholder
                inventory,
                score,
                inventory.getSellable(),
                0.0, // Mock distance
                0.0  // Mock cost
        );
    }

    // ─── DTOs ──────────────────────────────────────────────────────────────

    public record WarehouseAllocation(
            Warehouse warehouse,
            Inventory inventory,
            double score,
            int availableQuantity,
            double estimatedDistance,
            double estimatedCost
    ) {}
}
