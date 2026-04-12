package com.iemodo.marketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Marketing Service Application
 * 
 * Port: 8089
 * Provides coupon and promotion management services
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.iemodo.marketing",
        "com.iemodo.common"
})
public class MarketingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketingServiceApplication.class, args);
    }
}
