package com.dawn.booking.service;

import com.dawn.common.core.constant.ReservationStatus;

import java.util.Set;

// Centralized reservation state transitions.
public final class ReservationStateMachine {

    public enum Action {
        CONFIRM(ReservationStatus.CONFIRMED),
        CANCEL(ReservationStatus.CANCELED),
        FAIL(ReservationStatus.FAILED),
        EXPIRE(ReservationStatus.EXPIRED),
        FORCE_CANCEL(ReservationStatus.CANCELED);

        private final ReservationStatus targetStatus;

        Action(ReservationStatus targetStatus) {
            this.targetStatus = targetStatus;
        }

        public ReservationStatus targetStatus() {
            return targetStatus;
        }
    }

    private static final Set<ReservationStatus> RELEASE_FINAL_STATES =
            Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELED,
                    ReservationStatus.FAILED, ReservationStatus.EXPIRED,
                    ReservationStatus.REFUNDED);

    private ReservationStateMachine() {
    }

    // Null state (no DB row yet) always allows the action.
    public static boolean canTransition(ReservationStatus current, Action action) {
        if (current == null) {
            return true;
        }
        return switch (action) {
            case CONFIRM -> current == ReservationStatus.PENDING;
            case CANCEL, FAIL, EXPIRE -> !RELEASE_FINAL_STATES.contains(current);
            case FORCE_CANCEL -> true;
        };
    }
}