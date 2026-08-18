package com.dawn.booking.service;

import com.dawn.common.core.constant.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReservationStateMachine")
class ReservationStateMachineTest {

    @Test
    void confirm_onlyAllowedFromPendingOrMissingRow() {
        assertThat(ReservationStateMachine.canTransition(null, ReservationStateMachine.Action.CONFIRM)).isTrue();
        assertThat(ReservationStateMachine.canTransition(ReservationStatus.PENDING, ReservationStateMachine.Action.CONFIRM)).isTrue();
        for (ReservationStatus s : ReservationStatus.values()) {
            if (s != ReservationStatus.PENDING) {
                assertThat(ReservationStateMachine.canTransition(s, ReservationStateMachine.Action.CONFIRM)).isFalse();
            }
        }
    }

    @Test
    void release_skipsAllFinalStates() {
        ReservationStatus[] finalStates = {
                ReservationStatus.CONFIRMED, ReservationStatus.CANCELED,
                ReservationStatus.FAILED, ReservationStatus.EXPIRED,
                ReservationStatus.REFUNDED};
        for (ReservationStateMachine.Action a : new ReservationStateMachine.Action[]{
                ReservationStateMachine.Action.CANCEL,
                ReservationStateMachine.Action.FAIL,
                ReservationStateMachine.Action.EXPIRE}) {
            assertThat(ReservationStateMachine.canTransition(null, a)).isTrue();
            assertThat(ReservationStateMachine.canTransition(ReservationStatus.PENDING, a)).isTrue();
            for (ReservationStatus s : finalStates) {
                assertThat(ReservationStateMachine.canTransition(s, a)).isFalse();
            }
        }
    }

    @Test
    void forceCancel_allowedFromAnyState() {
        assertThat(ReservationStateMachine.canTransition(null, ReservationStateMachine.Action.FORCE_CANCEL)).isTrue();
        for (ReservationStatus s : ReservationStatus.values()) {
            assertThat(ReservationStateMachine.canTransition(s, ReservationStateMachine.Action.FORCE_CANCEL)).isTrue();
        }
    }
}