package com.iemodo.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregated SLA health endpoint showing status of all downstream services.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SlaHealthController {

    private final WebClient.Builder webClientBuilder;

    @Value("${iemodo.sla.timeout-seconds:5}")
    private int timeoutSeconds;

    @Value("${iemodo.services.tenant-management:http://tenant-management-service:8091}")
    private String tenantManagementUrl;

    @GetMapping("/sla/health")
    public Mono<ResponseEntity<Map<String, Object>>> slaHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gateway", "UP");
        result.put("timestamp", System.currentTimeMillis());

        // Check each downstream service
        Map<String, String> services = Map.of(
                "user-service", "http://user-service:8081",
                "product-service", "http://product-service:8082",
                "order-service", "http://order-service:8083",
                "inventory-service", "http://inventory-service:8086",
                "payment-service", "http://payment-service:8085",
                "tenant-management", tenantManagementUrl
        );

        return Mono.fromCallable(() -> {
            Map<String, Object> details = new LinkedHashMap<>();
            for (var entry : services.entrySet()) {
                try {
                    String status = checkService(entry.getValue());
                    details.put(entry.getKey(), status);
                } catch (Exception e) {
                    details.put(entry.getKey(), "DOWN");
                }
            }
            result.put("services", details);

            boolean allUp = details.values().stream().allMatch("UP"::equals);
            result.put("status", allUp ? "UP" : "DEGRADED");
            return ResponseEntity.ok(result);
        });
    }

    private String checkService(String baseUrl) {
        try {
            WebClient client = webClientBuilder.baseUrl(baseUrl).build();
            String result = client.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            return result != null && result.contains("\"status\":\"UP\"") ? "UP" : "DOWN";
        } catch (Exception e) {
            log.debug("SLA health check failed for {}: {}", baseUrl, e.getMessage());
            return "DOWN";
        }
    }
}
