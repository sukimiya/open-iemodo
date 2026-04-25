package com.iemodo.common.tenant;

import com.iemodo.common.exception.TenantNotFoundException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory;
import reactor.core.publisher.Mono;

/**
 * Reactive routing connection factory that selects the target
 * {@link io.r2dbc.spi.ConnectionFactory} based on the tenant ID stored
 * in the current Reactor Context.
 *
 * <p>The tenant ID is written into the context by {@link TenantIdWebFilter}
 * via the key {@link TenantContext#TENANT_ID_KEY}.
 *
 * <p>When no tenant ID is found in the Reactor Context (e.g. scheduled tasks,
 * message queue consumers), the {@link #systemTenantId} is used as fallback.
 *
 * <p>Registration of target connection factories is done in
 * {@code MultitenantR2dbcConfiguration} by calling
 * {@link #setTargetConnectionFactories(java.util.Map)}.
 */
@Slf4j
public class PostgresTenantConnectionFactory extends AbstractRoutingConnectionFactory {

    @Setter
    private String systemTenantId;

    @Override
    protected Mono<Object> determineCurrentLookupKey() {
        return Mono.deferContextual(Mono::just)
                .filter(ctx -> ctx.hasKey(TenantContext.TENANT_ID_KEY))
                .map(ctx -> ctx.get(TenantContext.TENANT_ID_KEY))
                .switchIfEmpty(Mono.defer(() -> {
                    if (systemTenantId != null) {
                        log.trace("No tenant in context, falling back to system tenant: {}", systemTenantId);
                        return Mono.just(systemTenantId);
                    }
                    return Mono.error(
                            new TenantNotFoundException("unknown — X-TenantID missing in context and no system-tenant-id configured")
                    );
                }))
                .doOnNext(tenantId -> log.debug("Routing connection to tenant: {}", tenantId));
    }
}
