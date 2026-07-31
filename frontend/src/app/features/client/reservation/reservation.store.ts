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
import { environment } from '@env/environment';
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

      const stopCountdown = () => {
        if (timerIdx) {
          clearInterval(timerIdx);
          timerIdx = null;
        }
      };

      const startCountdown = (ttl: number) => {
        stopCountdown();
        patchState(store, (state) => ({
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
                  next: ({ reservationId, ttl, expiredAt }) => {
                    patchState(store, {
                      reservationId: reservationId,
                      currentTTL: {
                        reservationId: reservationId,
                        ttl: ttl,
                        expiredAt: new Date(expiredAt),
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

                    startCountdown(ttl);

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
        holdSeats: () => {
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
          const api = store as unknown as {
            holdSeat: (args: { reservation: ReservationRequest }) => void;
          };
          patchState(store, { holdingSeats: true });
          api.holdSeat({ reservation: payload });
        },
        //
        pay: (radioValue: string) => {
          const resId = store.reservationId();
          const total = seatStore.totalPrice();
          const count = seatStore.selectSeatsCount();
          if (!resId || !total || !count) return;

          patchState(store, { paymentError: null, submittingPayment: true });

          const api = store as unknown as { confirm: (reservationId: string) => void };
          const doPayment = (paymentUrl: string) => {
            if (environment.useMock) {
              api.confirm(resId);
              storageService.removeItem('reservationState');
              patchState(store, { submittingPayment: false });
              router.navigateByUrl(paymentUrl);
            } else {
              document.defaultView?.open(paymentUrl, '_self');
            }
          };

          paymentService
            .createPayment({
              reservationId: resId,
              amount: total,
              paymentType: toPaymentMethod(radioValue),
            })
            .subscribe({
              next: doPayment,
              error: (err) => {
                patchState(store, {
                  submittingPayment: false,
                  paymentError:
                    err?.message || translate.instant('reservation.paymentFailed'),
                });
              },
            });
        },
        //
        holdSeat: rxMethod<{ reservation: ReservationRequest }>(
          pipe(
            tap(() =>
              patchState(store, { loading: true, saving: true, error: null }),
            ),
            switchMap(({ reservation }) => {
              return reservationService.holdReservationSeat(reservation).pipe(
                tapResponse({
                  next: () =>
                    patchState(store, {
                      loading: false,
                      saving: false,
                    }),
                  error: (err) =>
                    patchState(store, {
                      error: errorMessage(err),
                      loading: false,
                      saving: false,
                      holdingSeats: false,
                    }),
                }),
              );
            }),
          ),
        ),
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
                  next: ({ reservationId, ttl, expiredAt }) => {
                    patchState(store, {
                      reservationId: reservationId,
                      currentTTL: {
                        reservationId: reservationId,
                        ttl: ttl,
                        expiredAt: new Date(expiredAt),
                      },
                      saving: false,
                      loading: false,
                    });
                    startCountdown(ttl);
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
