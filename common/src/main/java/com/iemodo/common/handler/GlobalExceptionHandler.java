package com.iemodo.common.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iemodo.common.db.SlowQueryCircuitOpenException;
import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Global WebFlux exception handler.
 *
 * <p>Translates exceptions into the unified {@link Response} JSON format.
 * Must have a higher precedence than the default Spring Boot error handler
 * ({@code @Order(-1)}).
 */
@Slf4j
@Component
@Order(-1)
public class GlobalExceptionHandler implements WebExceptionHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        if (ex instanceof SlowQueryCircuitOpenException) {
            log.error("[CIRCUIT_OPEN] DB circuit breaker rejected request: {}", ex.getMessage());
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return writeResponse(response, Response.error(
                    HttpStatus.SERVICE_UNAVAILABLE.value(), "Service temporarily unavailable, please retry later"));
        }

        if (ex instanceof BusinessException be) {
            log.warn("Business exception [{}]: {}", be.getErrorCode(), be.getMessage());
            response.setStatusCode(be.getHttpStatus());
            return writeResponse(response, Response.error(be.getErrorCode(), be.getMessage()));
        }

        if (ex instanceof WebExchangeBindException bindEx) {
            String fieldErrors = bindEx.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            log.warn("Validation error: {}", fieldErrors);
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return writeResponse(response, Response.error(ErrorCode.BAD_REQUEST, fieldErrors));
        }

        if (ex instanceof ResponseStatusException rse) {
            response.setStatusCode(rse.getStatusCode());
            return writeResponse(response, Response.error(
                    rse.getStatusCode().value(), rse.getReason() != null ? rse.getReason() : ex.getMessage()));
        }

        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return writeResponse(response, Response.error(ErrorCode.INTERNAL_ERROR));
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, Response<?> body) {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            byte[] fallback = "{\"code\":500,\"message\":\"Internal server error\"}"
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(fallback);
            return response.writeWith(Mono.just(buffer));
        }
    }
}
