package com.iemodo.common.tenant;

import com.iemodo.common.exception.TenantNotFoundException;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.Map;

import static org.mockito.Mockito.mock;

@DisplayName("PostgresTenantConnectionFactory")
class PostgresTenantConnectionFactoryTest {

    private PostgresTenantConnectionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PostgresTenantConnectionFactory();
        // Wire up two mock tenant factories
        Map<Object, ConnectionFactory> targets = Map.of(
                "tenant_001", mock(ConnectionFactory.class),
                "tenant_002", mock(ConnectionFactory.class)
        );
        factory.setTargetConnectionFactories(targets);
        factory.afterPropertiesSet();
    }

    @Test
    @DisplayName("should resolve tenant key from Reactor Context")
    void shouldResolveTenantKey_whenContextContainsTenantId() {
        Mono<Object> lookup = factory.determineCurrentLookupKey()
                .contextWrite(Context.of(TenantContext.TENANT_ID_KEY, "tenant_001"));

        StepVerifier.create(lookup)
                .expectNext("tenant_001")
                .verifyComplete();
    }

    @Test
    @DisplayName("should error when Reactor Context has no tenant ID")
    void shouldError_whenContextMissingTenantId() {
        Mono<Object> lookup = factory.determineCurrentLookupKey();

        StepVerifier.create(lookup)
                .expectError(TenantNotFoundException.class)
                .verify();
    }
}
