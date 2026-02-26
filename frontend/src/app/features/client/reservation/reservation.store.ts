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
}

export const initialState: ReservationState = {
  reservation: null,
  reservationId: null,
  userId: null,
  currentTTL: null,
  loading: false,
  saving: false,
  error: null,
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
    ) => {
      let timerIdx: any = null;

      const stopCountdown = () => {
        if (timerIdx) {
          clearInterval(timerIdx);
          timerIdx = null;
        }
      };

      const startCountdown = (ttl: number) => {
        stopCountdown();
        console.log(`ttl: `, ttl);
        patchState(store, (state) => ({
          currentTTL: {
            reservationId: store.reservationId() || '',
            ttl: ttl,
            expiredAt: new Date(Date.now() + ttl * 1000),
          },
        }));

        timerIdx = setInterval(() => {
          patchState(store, (s) => {
            if (s.currentTTL && s.currentTTL.ttl > 0) {
              return {
                currentTTL: {
                  ...s.currentTTL,
                  ttl: s.currentTTL.ttl - 1,
                },
              };
            } else {
              stopCountdown();
              return {
                currentTTL: null,
                reservationId: null,
              };
            }
          });
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
                  error: (err: any) =>
                    patchState(store, { error: err.message, loading: false }),
                }),
              ),
            ),
          ),
        ),
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
                  error: (err: any) =>
                    patchState(store, {
                      error: err.message,
                      loading: false,
                      saving: false,
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
                  error: (err: any) => {
                    storageService.removeItem('reservationStore');
                    patchState(store, { error: err.message, saving: false });
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
