package com.iemodo.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Service Application
 * 
 * Port: 8085
 * Provides payment processing with Stripe/PayPal integration
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.iemodo.payment",
        "com.iemodo.common"
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
