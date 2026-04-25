package com.iemodo.tenant.controller;

import com.iemodo.common.response.Response;
import com.iemodo.tenant.dto.CreateTenantRequest;
import com.iemodo.tenant.dto.TenantDTO;
import com.iemodo.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST controller for tenant management.
 * 
 * <p>Base path: /api/v1/tenants
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * List all tenants.
     */
    @GetMapping
    public Mono<Response<List<TenantDTO>>> listTenants() {
        return tenantService.getAllTenants()
                .collectList()
                .map(Response::success);
    }

    /**
     * Get a specific tenant.
     */
    @GetMapping("/{tenantId}")
    public Mono<Response<TenantDTO>> getTenant(@PathVariable String tenantId) {
        return tenantService.getTenant(tenantId)
                .map(Response::success);
    }

    /**
     * Create a new tenant.
     */
    @PostMapping
    public Mono<Response<TenantDTO>> createTenant(
            @Valid @RequestBody CreateTenantRequest request) {
        return tenantService.createTenant(request)
                .map(Response::success);
    }

    /**
     * Suspend a tenant.
     */
    @PostMapping("/{tenantId}/suspend")
    public Mono<Response<TenantDTO>> suspendTenant(@PathVariable String tenantId) {
        return tenantService.suspendTenant(tenantId)
                .map(Response::success);
    }

    /**
     * Activate a suspended tenant.
     */
    @PostMapping("/{tenantId}/activate")
    public Mono<Response<TenantDTO>> activateTenant(@PathVariable String tenantId) {
        return tenantService.activateTenant(tenantId)
                .map(Response::success);
    }

    /**
     * Delete a tenant (soft delete).
     */
    @DeleteMapping("/{tenantId}")
    public Mono<Response<Void>> deleteTenant(@PathVariable String tenantId) {
        return tenantService.deleteTenant(tenantId)
                .then(Mono.just(Response.success()));
    }

    // ─── Configuration Endpoints ───────────────────────────────────────────

    /**
     * Get all configurations for a tenant.
     */
    @GetMapping("/{tenantId}/configs")
    public Mono<Response<Map<String, String>>> getTenantConfigs(
            @PathVariable String tenantId) {
        return tenantService.getTenantConfigs(tenantId)
                .map(Response::success);
    }

    /**
     * Update a tenant configuration.
     */
    @PutMapping("/{tenantId}/configs/{key}")
    public Mono<Response<Void>> updateTenantConfig(
            @PathVariable String tenantId,
            @PathVariable String key,
            @RequestBody String value) {
        return tenantService.updateTenantConfig(tenantId, key, value)
                .then(Mono.just(Response.success()));
    }
}
