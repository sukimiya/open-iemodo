package com.iemodo.rma.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RmaStatus state machine")
class RmaStatusTest {

    // ─── RETURN transitions ───────────────────────────────────────────────

    @ParameterizedTest(name = "RETURN: {0} -> {1} = {2}")
    @CsvSource({
            "PENDING_REVIEW,   APPROVED,          true",
            "PENDING_REVIEW,   REJECTED,          true",
            "PENDING_REVIEW,   CANCELLED,         true",
            "PENDING_REVIEW,   WAITING_SHIPMENT,  false",
            "APPROVED,         WAITING_SHIPMENT,  true",
            "APPROVED,         CANCELLED,         true",
            "APPROVED,         REFUNDING,         false",   // RETURN must go through shipment
            "WAITING_SHIPMENT, IN_TRANSIT,        true",
            "WAITING_SHIPMENT, CANCELLED,         true",
            "IN_TRANSIT,       RECEIVED,          true",
            "IN_TRANSIT,       REFUNDING,         false",   // must be received first
            "RECEIVED,         REFUNDING,         true",
            "RECEIVED,         INSPECTING,        false",   // INSPECTING is EXCHANGE only
            "REFUNDING,        COMPLETED,         true",
            "REFUNDING,        FAILED,            true",
            "COMPLETED,        REFUNDING,         false",   // terminal
            "REJECTED,         APPROVED,          false",   // terminal
            "CANCELLED,        APPROVED,          false",   // terminal
    })
    void returnTransitions(String from, String to, boolean expected) {
        assertThat(RmaStatus.valueOf(from).canTransitionTo(RmaStatus.valueOf(to), RmaType.RETURN))
                .isEqualTo(expected);
    }

    // ─── EXCHANGE transitions ─────────────────────────────────────────────

    @ParameterizedTest(name = "EXCHANGE: {0} -> {1} = {2}")
    @CsvSource({
            "RECEIVED,    INSPECTING,  true",
            "RECEIVED,    REFUNDING,   false",  // EXCHANGE must inspect first
            "INSPECTING,  RESHIPPING,  true",
            "INSPECTING,  REFUNDING,   true",   // inspection fail → refund
            "RESHIPPING,  COMPLETED,   true",
            "RESHIPPING,  REFUNDING,   false",  // can't refund after reshipping
    })
    void exchangeTransitions(String from, String to, boolean expected) {
        assertThat(RmaStatus.valueOf(from).canTransitionTo(RmaStatus.valueOf(to), RmaType.EXCHANGE))
                .isEqualTo(expected);
    }

    // ─── REFUND_ONLY transitions ──────────────────────────────────────────

    @ParameterizedTest(name = "REFUND_ONLY: {0} -> {1} = {2}")
    @CsvSource({
            "PENDING_REVIEW, APPROVED,          true",
            "PENDING_REVIEW, REJECTED,          true",
            "APPROVED,       REFUNDING,         true",
            "APPROVED,       WAITING_SHIPMENT,  false",  // no shipment for refund-only
            "REFUNDING,      COMPLETED,         true",
            "REFUNDING,      FAILED,            true",
    })
    void refundOnlyTransitions(String from, String to, boolean expected) {
        assertThat(RmaStatus.valueOf(from).canTransitionTo(RmaStatus.valueOf(to), RmaType.REFUND_ONLY))
                .isEqualTo(expected);
    }

    // ─── Terminal states ──────────────────────────────────────────────────

    @Test
    @DisplayName("Terminal states cannot transition to anything")
    void terminalStatesCannotTransition() {
        for (RmaStatus terminal : new RmaStatus[]{
                RmaStatus.REJECTED, RmaStatus.COMPLETED,
                RmaStatus.CANCELLED, RmaStatus.FAILED}) {
            assertThat(terminal.isTerminal()).isTrue();
            for (RmaType type : RmaType.values()) {
                for (RmaStatus target : RmaStatus.values()) {
                    assertThat(terminal.canTransitionTo(target, type))
                            .as("%s -> %s [%s] should be false", terminal, target, type)
                            .isFalse();
                }
            }
        }
    }

    // ─── RmaRequest.transitionTo ──────────────────────────────────────────

    @Test
    @DisplayName("RmaRequest.transitionTo updates status and sets timestamps")
    void rmaRequestTransitionSetsTimestamp() {
        RmaRequest rma = RmaRequest.builder()
                .rmaNo("RMA001")
                .type(RmaType.RETURN)
                .rmaStatus(RmaStatus.PENDING_REVIEW)
                .regionSnapshot("{}")
                .build();

        rma.transitionTo(RmaStatus.APPROVED, 42L);

        assertThat(rma.getRmaStatus()).isEqualTo(RmaStatus.APPROVED);
        assertThat(rma.getApprovedAt()).isNotNull();
        assertThat(rma.getLastOperatorId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("RmaRequest.transitionTo throws on illegal transition")
    void rmaRequestTransitionThrowsOnIllegalMove() {
        RmaRequest rma = RmaRequest.builder()
                .rmaNo("RMA001")
                .type(RmaType.RETURN)
                .rmaStatus(RmaStatus.PENDING_REVIEW)
                .regionSnapshot("{}")
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> rma.transitionTo(RmaStatus.RESHIPPING, 1L));
    }

    @Test
    @DisplayName("RmaRequest.transitionTo throws when already terminal")
    void rmaRequestTransitionThrowsWhenTerminal() {
        RmaRequest rma = RmaRequest.builder()
                .rmaNo("RMA001")
                .type(RmaType.RETURN)
                .rmaStatus(RmaStatus.COMPLETED)
                .regionSnapshot("{}")
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> rma.transitionTo(RmaStatus.REFUNDING, 1L));
    }
}
