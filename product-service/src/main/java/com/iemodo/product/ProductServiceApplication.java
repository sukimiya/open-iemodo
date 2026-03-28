package com.iemodo.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Product Catalog Service - Manages product catalog with multi-tenant support.
 * 
 * <p>Features:
 * <ul>
 *   <li>Product and SKU management
 *   <li>Category and brand management
 *   <li>Internationalization (multi-language, country visibility)
 *   <li>Regional pricing
 *   <li>Cache integration
 * </ul>
 * 
 * <p>Port: 8082
 * <p>Schema: product_{tenantId}
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.iemodo.product", "com.iemodo.common"})
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
