package com.iemodo.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Replaces the default {@link SentinelGatewayBlockExceptionHandler} with one that:
 * <ul>
 *   <li>Returns structured JSON instead of an HTML/plain-text response</li>
 *   <li>Distinguishes rate-limited (429) from circuit-open (503) responses</li>
 *   <li>Logs circuit-open events for alerting</li>
 * </ul>
 */
@Slf4j
public class CustomBlockExceptionHandler extends SentinelGatewayBlockExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomBlockExceptionHandler(List<ViewResolver> viewResolvers,
                                       ServerCodecConfigurer serverCodecConfigurer) {
        super(viewResolvers, serverCodecConfigurer);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        if (!(ex instanceof BlockException blockEx)) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String errorCode;
        String message;

        if (blockEx instanceof DegradeException) {
            status    = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "SERVICE_DEGRADED";
            message   = "Service is temporarily unavailable, please retry later";
            log.warn("[CIRCUIT_OPEN] path={} rule={}",
                    exchange.getRequest().getPath(), blockEx.getRule());
        } else {
            status    = HttpStatus.TOO_MANY_REQUESTS;
            errorCode = "TOO_MANY_REQUESTS";
            message   = "Request rate limit exceeded, please slow down";
        }

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "code", status.value(),
                    "errorCode", errorCode,
                    "message", message
            ));
            DataBuffer buffer = response.bufferFactory().wrap(body);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
