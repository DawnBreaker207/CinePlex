package com.dawn.booking.service;

import com.dawn.common.core.constant.ReservationStatus;

import java.util.Set;

/**
 * Centralized reservation state transitions (Phase 4).
 * Behavior mirrors the guards that previously lived inline in
 * ReservationLifecycleServiceImpl — no observable change.
 */
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

    /**
     * @param current current persisted state, or {@code null} when no DB row exists
     * @return {@code true} if the action may proceed
     */
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