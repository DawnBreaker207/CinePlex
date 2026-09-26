import {
  Reservation,
  ReservationInitRequest,
  ReservationRequest,
} from '@domain/reservation/models/reservation.model';
import {
  patchState,
  signalStore,
  withComputed,
  withHooks,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { ReservationService } from '@domain/reservation/data-access/reservation.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { StorageService } from '@core/services/storage/storage.service';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { DOCUMENT } from '@angular/common';
import { PaymentService } from '@domain/payment/data-access/payment.service';
import { PaymentMethod } from '@core/constants/enum';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';
import { selectJwt } from '@core/auth/auth.selectors';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { selectSelectedTheater } from '@domain/theater/data-access/theater.selectors';
import { TranslateService } from '@ngx-translate/core';
import { errorMessage } from '@shared/utils/error.helper';

function toPaymentMethod(value: string): PaymentMethod {
  switch (value) {
    case 'momo':
      return 'MOMO';
    case 'zalopay':
      return 'ZALOPAY';
    case 'cash':
      return 'CASH';
    case 'mock':
      return 'MOCK';
    case 'atm':
    case 'visa':
    case 'vnpay':
      return 'VNPAY';
    default:
      return 'VNPAY';
  }
}

export interface ReservationState {
  reservation: Reservation | null;
  reservationId: string | null;
  userId: number | null;
  loading: boolean;
  currentTTL: {
    reservationId: string;
    ttl: number;
    expiredAt: Date;
  } | null;
  saving: boolean;
  error: string | null;
  holdingSeats: boolean;
  submittingPayment: boolean;
  paymentError: string | null;
}

export const initialState: ReservationState = {
  reservation: null,
  reservationId: null,
  userId: null,
  currentTTL: null,
  loading: false,
  saving: false,
  error: null,
  holdingSeats: false,
  submittingPayment: false,
  paymentError: null,
};

export const ReservationStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    remainingTime: computed(() => store.currentTTL()?.ttl),

    formattedTTL: computed(() => {
      const ttl = store.currentTTL()?.ttl;
      if (!ttl) return '00:00';
      const m = Math.floor(ttl / 60);
      const s = ttl % 60;
      return `${m}:${s.toString().padStart(2, '0')}`;
    }),
  })),
  withMethods(
    (
      store,
      reservationService = inject(ReservationService),
      storageService = inject(StorageService),
      router = inject(Router),
      storeNgrx = inject(Store),
      document = inject(DOCUMENT),
      seatStore = inject(SeatStore),
      paymentService = inject(PaymentService),
      translate = inject(TranslateService),
    ) => {
      let timerIdx: ReturnType<typeof setInterval> | null = null;
      // S1: silent hold refresh while the user is still on the booking page.
      // Interaction-only TTL refresh used to expire the hold mid-session.
      let heartbeat: ReturnType<typeof setInterval> | null = null;
      const HEARTBEAT_MS = 45_000;

      const stopCountdown = () => {
        if (timerIdx) {
          clearInterval(timerIdx);
          timerIdx = null;
        }
        stopHeartbeat();
      };

      const stopHeartbeat = () => {
        if (heartbeat) {
          clearInterval(heartbeat);
          heartbeat = null;
        }
      };

      const startHeartbeat = () => {
        stopHeartbeat();
        heartbeat = setInterval(() => {
          const resId = store.reservationId();
          if (
            !resId ||
            seatStore.selectSeatsCount() === 0 ||
            store.submittingPayment()
          ) {
            return;
          }
          holdSeatsImpl(true);
        }, HEARTBEAT_MS);
      };

      const startCountdown = (ttl: number) => {
        stopCountdown();
        patchState(store, () => ({
          currentTTL: {
            reservationId: store.reservationId() || '',
            ttl: ttl,
            expiredAt: new Date(Date.now() + ttl * 1000),
          },
        }));

        timerIdx = setInterval(() => {
          const ttl = store.currentTTL()?.ttl;
          if (!ttl || ttl <= 0) {
            stopCountdown();
            patchState(store, {
              currentTTL: null,
              reservationId: null,
              error: translate.instant('reservation.expired'),
            });
            router.navigate(['/home']);
            return;
          }
          patchState(store, (s) => ({
            currentTTL: s.currentTTL
              ? { ...s.currentTTL, ttl: s.currentTTL.ttl - 1 }
              : null,
          }));
        }, 1000);
      };

      const holdSeatsImpl = (silent = false) => {
        const selectedSeats = seatStore.selectedSeats();
        const user = storeNgrx.selectSignal(selectJwt)();
        const showtime = storeNgrx.selectSignal(selectSelectedShowtime)();
        const theater = storeNgrx.selectSignal(selectSelectedTheater)();
        const resId = store.reservationId();

        if (
          !selectedSeats.length ||
          !user ||
          !showtime ||
          !resId ||
          !theater
        ) {
          return;
        }

        const payload: ReservationRequest = {
          reservationId: resId,
          userId: user.userId,
          showtimeId: showtime.id,
          seatIds: selectedSeats.map((s) => s.id),
        };

        storageService.setItem('reservationState', {
          ...payload,
          theaterId: theater.id,
        });
        if (!silent) {
          patchState(store, {
            holdingSeats: true,
            loading: true,
            saving: true,
            error: null,
          });
        }
        reservationService.holdReservationSeat(payload).subscribe({
          next: () => {
            if (!silent) patchState(store, { loading: false, saving: false });
          },
          error: (err) => {
            // S1/S4: a silent refresh that finds a dead hold surfaces the
            // expiry instead of looping quietly; the countdown path already
            // navigates home from this state.
            if (silent) stopHeartbeat();
            patchState(store, {
              error: errorMessage(err),
              loading: false,
              saving: false,
              holdingSeats: false,
            });
          },
        });
      };

      return {
        //
        initReservation: rxMethod<ReservationInitRequest>(
          pipe(
            tap(() =>
              patchState(store, { loading: true, saving: true, error: null }),
            ),
            switchMap((req) =>
              reservationService.initReservation(req).pipe(
                tapResponse({
                  next: (res) => {
                    const reservationId =
                      res.reservationCode ?? res.reservationId;
                    patchState(store, {
                      reservationId: reservationId,
                      currentTTL: {
                        reservationId: reservationId,
                        ttl: res.ttl,
                        expiredAt: new Date(res.expiredAt),
                      },
                      saving: false,
                      loading: false,
                    });

                    storageService.setItem('reservationState', {
                      reservationId,
                      userId: req.userId,
                      showtimeId: req.showtimeId,
                      theaterId: req.theaterId,
                    });

                    startCountdown(res.ttl);
                    startHeartbeat();

                    router.navigate([
                      `/reservation/${reservationId}/${req.showtimeId}`,
                    ]);
                  },
                  error: (err) =>
                    patchState(store, { error: errorMessage(err), loading: false }),
                }),
              ),
            ),
          ),
        ),
        //
        holdSeats: (silent = false) => holdSeatsImpl(silent),
        //
        pay: (radioValue: string, voucherCode?: string, comboAmount = 0) => {
          const resId = store.reservationId();
          const total = seatStore.totalPrice() + comboAmount;
          const count = seatStore.selectSeatsCount();
          if (!resId || !total || !count) return;

          patchState(store, { paymentError: null, submittingPayment: true });
          storageService.setItem('lastPayment', {
            provider: toPaymentMethod(radioValue),
            reservationId: resId,
          });

          const fail = (err: unknown) => {
            // The payment never started: drop the stale marker so a later
            // reconnect does not nag about a payment that never existed.
            storageService.removeItem('lastPayment');
            const status = (err as { status?: number })?.status;
            if (status === 409) {
              // S4: hold died between seat select and payment (sleep/expired
              // TTL). Drop the dead session so the next booking starts clean.
              storageService.removeItem('reservationState');
              stopCountdown();
              patchState(store, {
                submittingPayment: false,
                reservationId: null,
                currentTTL: null,
                paymentError: translate.instant('reservation.expired'),
              });
              router.navigate(['/home']);
              return;
            }
            patchState(store, {
              submittingPayment: false,
              paymentError:
                errorMessage(err) ||
                translate.instant('reservation.paymentFailed'),
            });
          };

          const doPayment = (paymentUrl: string) => {
            document.defaultView?.open(paymentUrl, '_self');
          };
          const startPayment = () =>
            paymentService
              .createPayment({
                reservationId: resId,
                amount: total,
                paymentType: toPaymentMethod(radioValue),
                seatIds: seatStore.selectedSeats().map((s) => s.id),
              })
              .subscribe({ next: doPayment, error: fail });

          if (voucherCode) {
            reservationService
              .applyVoucher(resId, voucherCode)
              .subscribe({ next: () => startPayment(), error: fail });
          } else {
            startPayment();
          }
        },
        //
        confirm: (reservationId: string) => {
          stopCountdown();
          patchState(store, {
            reservationId: reservationId,
            currentTTL: null,
            loading: false,
            saving: false,
            error: null,
          });
        },
        //
        cancel: () => {
          stopCountdown();
          patchState(store, initialState);
        },
        //
        restore: rxMethod<{ reservationId: string }>(
          pipe(
            tap(() => patchState(store, { loading: true, saving: true })),
            switchMap(({ reservationId }) =>
              reservationService.restoreReservation(reservationId).pipe(
                tapResponse({
                  next: (res) => {
                    const reservationId =
                      res.reservationCode ?? res.reservationId;
                    patchState(store, {
                      reservationId: reservationId,
                      currentTTL: {
                        reservationId: reservationId,
                        ttl: res.ttl,
                        expiredAt: new Date(res.expiredAt),
                      },
                      saving: false,
                      loading: false,
                    });
                    startCountdown(res.ttl);
                    startHeartbeat();
                  },
                  error: (err) => {
                    storageService.removeItem('reservationState');
                    patchState(store, { error: errorMessage(err), saving: false });
                    router.navigate(['/home']);
                  },
                }),
              ),
            ),
          ),
        ),
        //
        startCountdown,
        //
        stopCountdown,
        //
        _cleanup: () => stopCountdown(),
        //
        updateTTL: (ttl: number) => {
          const current = store.currentTTL();
          if (current) patchState(store, { currentTTL: { ...current, ttl } });
        },
        //
        resetHolding: () => patchState(store, { holdingSeats: false }),
        //
        clearError: () => patchState(store, { error: null }),
      };
    },
  ),
  withHooks({
    // onInit(store) {},
    onDestroy(store) {
      store._cleanup();
    },
  }),
);
