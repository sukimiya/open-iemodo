package com.iemodo.common.db;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * A {@link ConnectionFactory} decorator that gates connection creation behind
 * the {@link SlowQueryCircuitBreaker}.
 *
 * <p>When the circuit is OPEN, {@link #create()} immediately returns a
 * {@link Mono#error} with {@link SlowQueryCircuitOpenException} instead of
 * attempting a real DB connection. This prevents query pile-up during a
 * slow-query storm and lets the downstream service degrade gracefully.
 *
 * <p>Layering:
 * <pre>
 *   CircuitBreakerConnectionFactory  ← this class (connection-level gate)
 *     └── ProxyConnectionFactory     ← slow-query timing via r2dbc-proxy
 *           └── PostgresqlConnectionFactory
 * </pre>
 */
public class CircuitBreakerConnectionFactory implements ConnectionFactory {

    private final ConnectionFactory delegate;
    private final SlowQueryCircuitBreaker circuitBreaker;

    public CircuitBreakerConnectionFactory(ConnectionFactory delegate,
                                            SlowQueryCircuitBreaker circuitBreaker) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Publisher<? extends Connection> create() {
        try {
            circuitBreaker.checkAndThrowIfOpen();
        } catch (SlowQueryCircuitOpenException ex) {
            return Mono.error(ex);
        }
        return delegate.create();
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return delegate.getMetadata();
    }
}
