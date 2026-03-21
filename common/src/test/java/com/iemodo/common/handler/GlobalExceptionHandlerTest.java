package com.iemodo.common.handler;

import com.iemodo.common.exception.BusinessException;
import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.exception.TenantIdMissingException;
import com.iemodo.common.exception.TenantNotFoundException;
import com.iemodo.common.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build());
    }

    @Test
    @DisplayName("BusinessException (TenantIdMissing) → 400 + TENANT_ID_MISSING code")
    void shouldReturn400_forTenantIdMissingException() {
        MockServerWebExchange exchange = exchange();
        TenantIdMissingException ex = new TenantIdMissingException();

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("BusinessException (TenantNotFound) → 400 + TENANT_NOT_FOUND code")
    void shouldReturn400_forTenantNotFoundException() {
        MockServerWebExchange exchange = exchange();
        TenantNotFoundException ex = new TenantNotFoundException("tenant_xyz");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("BusinessException (InsufficientStock) → 409")
    void shouldReturn409_forInsufficientStockException() {
        MockServerWebExchange exchange = exchange();
        InsufficientStockException ex = new InsufficientStockException("SKU-001");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("ResponseStatusException → mapped HTTP status")
    void shouldMapResponseStatusException() {
        MockServerWebExchange exchange = exchange();
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Unhandled exception → 500 INTERNAL_ERROR")
    void shouldReturn500_forUnhandledException() {
        MockServerWebExchange exchange = exchange();
        RuntimeException ex = new RuntimeException("unexpected error");

        StepVerifier.create(handler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Response body has application/json content type")
    void shouldSetJsonContentType() {
        MockServerWebExchange exchange = exchange();
        handler.handle(exchange, new TenantIdMissingException()).block();

        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isNotNull()
                .hasToString("application/json");
    }
}
