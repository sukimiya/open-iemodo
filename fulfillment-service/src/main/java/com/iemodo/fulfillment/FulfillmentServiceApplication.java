package com.iemodo.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Fulfillment Service Application
 * 
 * Port: 8092
 * Provides warehouse allocation and logistics optimization services
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iemodo.fulfillment",
        "com.iemodo.common"
})
public class FulfillmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FulfillmentServiceApplication.class, args);
    }
}
