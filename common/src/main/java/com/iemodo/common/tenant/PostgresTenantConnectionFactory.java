package com.iemodo.common.tenant;

import com.iemodo.common.exception.TenantNotFoundException;
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
 * <p>Registration of target connection factories is done in
 * {@code MultitenantR2dbcConfiguration} by calling
 * {@link #setTargetConnectionFactories(java.util.Map)}.
 */
@Slf4j
public class PostgresTenantConnectionFactory extends AbstractRoutingConnectionFactory {

    @Override
    protected Mono<Object> determineCurrentLookupKey() {
        return Mono.deferContextual(Mono::just)
                .filter(ctx -> ctx.hasKey(TenantContext.TENANT_ID_KEY))
                .map(ctx -> {
                    Object tenantId = ctx.get(TenantContext.TENANT_ID_KEY);
                    log.debug("Routing connection to tenant: {}", tenantId);
                    return tenantId;
                })
                .switchIfEmpty(Mono.error(
                        new TenantNotFoundException("unknown — X-TenantID missing in context")
                ));
    }
}
