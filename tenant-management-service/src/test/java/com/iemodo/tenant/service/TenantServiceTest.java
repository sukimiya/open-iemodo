package com.iemodo.tenant.service;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.tenant.domain.Tenant;
import com.iemodo.tenant.dto.CreateTenantRequest;
import com.iemodo.tenant.dto.TenantDTO;
import com.iemodo.tenant.repository.TenantConfigRepository;
import com.iemodo.tenant.repository.TenantRepository;
import com.iemodo.tenant.repository.TenantSchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("TenantService")
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantSchemaRepository schemaRepository;
    @Mock private TenantConfigRepository configRepository;
    @Mock private TenantProvisioningService provisioningService;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantService = new TenantService(tenantRepository, schemaRepository, configRepository, provisioningService);
    }

    @Test
    @DisplayName("getTenant: should return tenant when exists")
    void getTenant_shouldSucceed_whenExists() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .tenantId("acme-corp")
                .tenantName("Acme Corporation")
                .tenantCode("acme")
                .status("ACTIVE")
                .planType("STANDARD")
                .createdAt(Instant.now())
                .build();

        when(tenantRepository.findByTenantId("acme-corp")).thenReturn(Mono.just(tenant));
        when(configRepository.findAllByTenantId("acme-corp")).thenReturn(Flux.empty());

        StepVerifier.create(tenantService.getTenant("acme-corp"))
                .assertNext(dto -> {
                    assertThat(dto.getTenantId()).isEqualTo("acme-corp");
                    assertThat(dto.getTenantName()).isEqualTo("Acme Corporation");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTenant: should fail with TENANT_NOT_FOUND when not exists")
    void getTenant_shouldFail_whenNotExists() {
        when(tenantRepository.findByTenantId("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(tenantService.getTenant("unknown"))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException be
                        && be.getErrorCode() == ErrorCode.TENANT_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("createTenant: should create tenant when id not taken")
    void createTenant_shouldSucceed_whenIdNotTaken() {
        CreateTenantRequest request = new CreateTenantRequest();
        request.setTenantId("new-corp");
        request.setTenantName("New Corporation");
        request.setTenantCode("new");
        request.setPlanType("PROFESSIONAL");

        Tenant savedTenant = Tenant.builder()
                .id(1L)
                .tenantId("new-corp")
                .tenantName("New Corporation")
                .tenantCode("new")
                .status("ACTIVE")
                .planType("PROFESSIONAL")
                .createdAt(Instant.now())
                .build();

        when(tenantRepository.existsByTenantId("new-corp")).thenReturn(Mono.just(false));
        when(tenantRepository.existsByTenantCode("new")).thenReturn(Mono.just(false));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(Mono.just(savedTenant));
        when(schemaRepository.save(any())).thenReturn(Mono.just(org.mockito.Mockito.mock()));
        when(configRepository.save(any())).thenReturn(Mono.just(org.mockito.Mockito.mock()));
        when(provisioningService.provisionSchemas(any())).thenReturn(Mono.empty());

        StepVerifier.create(tenantService.createTenant(request))
                .assertNext(dto -> {
                    assertThat(dto.getTenantId()).isEqualTo("new-corp");
                    assertThat(dto.getStatus()).isEqualTo("ACTIVE");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("suspendTenant: should suspend active tenant")
    void suspendTenant_shouldSucceed_whenActive() {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .tenantId("acme-corp")
                .status("ACTIVE")
                .build();

        Tenant suspendedTenant = Tenant.builder()
                .id(1L)
                .tenantId("acme-corp")
                .status("SUSPENDED")
                .build();

        when(tenantRepository.findByTenantId("acme-corp")).thenReturn(Mono.just(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(Mono.just(suspendedTenant));

        StepVerifier.create(tenantService.suspendTenant("acme-corp"))
                .assertNext(dto -> assertThat(dto.getStatus()).isEqualTo("SUSPENDED"))
                .verifyComplete();
    }
}
