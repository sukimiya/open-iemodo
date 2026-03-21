package com.iemodo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway entry point.
 *
 * <p>Excludes R2DBC auto-configuration — the gateway itself does not
 * connect to a database; it delegates all persistence to downstream services.
 */
@SpringBootApplication(exclude = {R2dbcAutoConfiguration.class})
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
