package com.iemodo.tax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tax Service Application
 * 
 * Port: 8088
 * Provides global tax compliance and calculation services
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iemodo.tax",
        "com.iemodo.common"
})
public class TaxServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxServiceApplication.class, args);
    }
}
