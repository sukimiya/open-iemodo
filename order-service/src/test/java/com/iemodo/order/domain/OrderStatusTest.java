package com.iemodo.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderStatus state machine")
class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} should be {2}")
    @CsvSource({
            "PENDING_PAYMENT, PAID,            true",
            "PENDING_PAYMENT, CANCELLED,       true",
            "PENDING_PAYMENT, PROCESSING,      false",
            "PENDING_PAYMENT, SHIPPED,         false",
            "PAID,            PROCESSING,      true",
            "PAID,            REFUNDED,        true",
            "PAID,            SHIPPED,         false",
            "PROCESSING,      SHIPPED,         true",
            "PROCESSING,      REFUNDED,        true",
            "PROCESSING,      PAID,            false",
            "SHIPPED,         DELIVERED,       true",
            "SHIPPED,         REFUNDED,        false",
            "DELIVERED,       REFUNDED,        false",
            "CANCELLED,       PAID,            false",
            "REFUNDED,        PAID,            false",
    })
    void stateMachineTransitions(String from, String to, boolean expected) {
        OrderStatus fromStatus = OrderStatus.valueOf(from);
        OrderStatus toStatus   = OrderStatus.valueOf(to);
        assertThat(fromStatus.canTransitionTo(toStatus)).isEqualTo(expected);
    }

    @Test
    @DisplayName("DELIVERED is a terminal state")
    void deliveredIsTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.DELIVERED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    @DisplayName("CANCELLED is a terminal state")
    void cancelledIsTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }
}
