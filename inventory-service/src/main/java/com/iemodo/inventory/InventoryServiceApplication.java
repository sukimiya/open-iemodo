package com.iemodo.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Inventory Management Service - Multi-tenant inventory management with:
 * 
 * <p>Features:
 * <ul>
 *   <li>Multi-warehouse support
 *   <li>Real-time stock tracking
 *   <li>Redis-based anti-overselling
 *   <li>Smart warehouse allocation
 *   <li>Stock transfer management
 * </ul>
 * 
 * <p>Port: 8084
 * <p>Schema: inventory_{tenantId}
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.iemodo.inventory", "com.iemodo.common"})
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
