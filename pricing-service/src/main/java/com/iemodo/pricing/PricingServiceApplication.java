package com.iemodo.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/**
 * Pricing Service Application
 * 
 * Port: 8086
 * Provides multi-currency pricing and exchange rate services
 */
@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {
        "com.iemodo.pricing",
        "com.iemodo.common"
})
public class PricingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingServiceApplication.class, args);
    }
}
