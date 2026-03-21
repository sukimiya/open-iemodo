package com.iemodo.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Gateway-level error handler.
 *
 * <p>Catches exceptions that escape the filter chain (e.g. no route found,
 * service unavailable) and returns a consistent {@link Response} JSON body.
 *
 * <p>Order {@code -2} — higher priority than Spring Boot's default
 * {@code DefaultErrorWebExceptionHandler} (order {@code -1}).
 */
@Slf4j
@Component
@Order(-2)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status;
        Response<?> body;

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            body = Response.error(ErrorCode.NOT_FOUND, "No route found for the requested path");
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            body = Response.error(status.value(), rse.getReason() != null ? rse.getReason() : ex.getMessage());
        } else {
            log.error("Gateway unhandled error [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            body = Response.error(ErrorCode.INTERNAL_ERROR);
        }

        response.setStatusCode(status);
        return write(response, body);
    }

    private Mono<Void> write(ServerHttpResponse response, Response<?> body) {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] fallback = "{\"code\":500,\"message\":\"Internal server error\"}"
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}
