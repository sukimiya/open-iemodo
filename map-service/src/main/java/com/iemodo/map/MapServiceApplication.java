package com.iemodo.map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Map Service Application
 * 
 * Port: 8087
 * Provides geocoding, routing, and distance calculation services
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iemodo.map",
        "com.iemodo.common"
})
public class MapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapServiceApplication.class, args);
    }
}
