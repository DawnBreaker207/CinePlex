# Reservation State Machine

> Describes the ACTUAL behavior of the code (module `dawn-booking`), not an idealized design.
> Enum: `com.dawn.common.core.constant.ReservationStatus`, stored via `@Enumerated(EnumType.STRING)`.
> `isPaid` is a separate column (boolean), not derived from state on write — only on read
> (`CONFIRMED || REFUNDED` → `isPaid=true`).

## 1. States

| State | Meaning |
|---|---|
| `PENDING` | Seat is being held (Redis lock), not yet paid. DB row upserted in `holdReservationSeats` with `expiredAt = now + HOLD_TIMEOUT`. |
| `CONFIRMED` | Payment succeeded, seat is booked (`seat_status = BOOKED`), `isPaid = true`. |
| `CANCELED` | Canceled — seat released, voucher released (if any). |
| `FAILED` | Payment failed (`PaymentFailedEvent`) — handled the same way as cancel. |
| `EXPIRED` | Seat hold time expired — set by the job that scans `expiredAt`. |
| `REFUNDED` | **Dead state** — no code ever transitions into this state; used only to compute `isPaid` on read. |

## 2. Transition table

| From | To | Trigger | Guard | Code |
|---|---|---|---|---|
| *(no row)* | `PENDING` | `holdReservationSeats` (POST `/reservation/seatHold`) | Redis session + ownership + showtime + seat validity, Redis lock acquired successfully. **Does not check the current DB row status** — the upsert overwrites everything. | `ReservationServiceImpl.holdReservationSeats` |
| `PENDING` | `CONFIRMED` | POST `/reservation/confirm/{id}`, or `PaymentCompletedEvent` → `PaymentCompletedListener.onPaymentCompleted` | If already `CONFIRMED` → idempotent, returns the existing record. State ≠ `PENDING` → 409. **No DB row** → guard is skipped, confirmation relies entirely on the Redis session. There is a processing lock against concurrent execution plus a CAS `bookSeats`. | `ReservationServiceImpl.confirmReservation` |
| `PENDING` | `CANCELED` | POST `/reservation/{id}/cancel`, or the AI tool (non-CONFIRMED branch) | If state ∈ {`CONFIRMED`,`CANCELED`,`FAILED`,`EXPIRED`} → silently skipped (log warn + return 200, no throw). **`REFUNDED` is not in the skip-list.** | `ReservationServiceImpl.cancelReservation` → `releaseReservation` |
| `PENDING` | `FAILED` | `PaymentFailedEvent` → `onPaymentFailed` | Same guard as cancel (same missing `REFUNDED`). | `ReservationServiceImpl.failReservation` |
| `PENDING` | `EXPIRED` | `service/ExpirationJob` (via `expireReservation`, has guard/Redis cleanup/broadcast/audit) — the duplicate job `job/ReservationExpirationJob` has been removed | same guard as cancel | `ExpirationJob.expireReservation` |
| `CONFIRMED` | `CANCELED` | AI tool `cancelAndRefundReservation` (admin, CONFIRMED branch) | No state guard, sets directly + `isDeleted=true`. Refund is not automatic (handled manually via the payment gateway). | `SupportTools.cancelAndRefundReservation` → `forceCancelReservation` |
| — | `REFUNDED` | — | **No transition exists that sets this state.** | — |

**Side effects per transition** (must be preserved when refactoring):
- → `PENDING`: SSE broadcast seat-hold; audit `RESERVATION_HOLD`.
- `PENDING` → `CONFIRMED`: CAS `bookSeats`; `useVoucher`; email; `cleanupRedisLocks`; audit; compensating unbook if save fails.
- → `CANCELED`/`FAILED`/`EXPIRED` (release): removes Redis seat locks + session; `unbookSeats` (CAS); SSE broadcast seat-release; `releaseVoucher`; audit (always logs `from=null`, even when a row already exists).
- `CONFIRMED` → `CANCELED` (force): `unbookSeats` + removes Redis locks + deletes the reservation; **no audit, no broadcast, no voucher release**.

## 3. Known bugs / missing guards

1. ~~Two expiration jobs running concurrently~~ — `job/ReservationExpirationJob` has been removed, only `service/ExpirationJob` remains.
2. **`holdReservationSeats`'s upsert does not check the current DB status** — `save()` overwrites `reservationStatus` even if the row is already `CONFIRMED`. Indirectly mitigated by `cleanupRedisLocks` removing the session after confirmation, but there is still a small race window between confirm and cleanup.
3. **`releaseReservation`'s skip-list is missing `REFUNDED`** — currently harmless (nothing ever sets `REFUNDED`), but needs to be closed once the state machine is centralized.
4. **`confirmReservation` skips the guard when there is no DB row** — relies entirely on the Redis session; needs confirmation on whether this is intentional (session-first design).
5. **A user canceling their own already-`CONFIRMED` reservation is silently ignored**, returning 200 even though nothing happened — this can easily mislead the client.
6. **`forceCancelReservation` has no guard/audit/voucher-release/broadcast** — the manual refund is intentional, but the rest may be an oversight.
7. **`applyVoucher` does not null-check the Redis session** — throws NPE if the session has expired/been removed, instead of a clear business error.
8. **`REFUNDED` has never been implemented** — no refund flow exists that sets this state.

## 4. Controls that are known to work correctly (keep as-is)

- Redis processing lock preventing concurrent confirm/release.
- CAS `bookSeats`/`unbookSeats` into the cinema module (atomic seat booking/release).
- Idempotent confirm: a row already `CONFIRMED` → returns the existing data, does not create a new one.
- Compensation: if `saveAndFlush` fails during confirm → `unbookSeats` rolls back.
