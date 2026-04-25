package com.iemodo.common.db;

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;

/**
 * R2DBC proxy listener that measures query execution time and feeds the
 * result into {@link SlowQueryCircuitBreaker}.
 *
 * <p>Wired via {@link io.r2dbc.proxy.ProxyConnectionFactory}.
 */
public class SlowQueryProxyListener implements ProxyExecutionListener {

    private final SlowQueryProperties props;
    private final SlowQueryCircuitBreaker circuitBreaker;

    public SlowQueryProxyListener(SlowQueryProperties props,
                                   SlowQueryCircuitBreaker circuitBreaker) {
        this.props = props;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void beforeQuery(QueryExecutionInfo info) {
        // no-op: circuit check happens at ConnectionFactory.create() level
    }

    @Override
    public void afterQuery(QueryExecutionInfo info) {
        long elapsedMs = info.getExecuteDuration().toMillis();

        if (elapsedMs >= props.getThresholdMs()) {
            String sql = info.getQueries().stream()
                    .map(QueryInfo::getQuery)
                    .findFirst()
                    .orElse("unknown");
            circuitBreaker.recordSlowQuery(elapsedMs, sql);
        } else if (info.isSuccess()) {
            circuitBreaker.recordFastQuery();
        }
    }
}
