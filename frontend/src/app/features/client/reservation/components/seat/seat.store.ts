import { Seat } from '@domain/seat/models/seat.model';
import { buildFixedRow, trimOuterEmptyCells } from '@shared/utils/seat-layout';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { SeatService } from '@domain/seat/data-access/seat.service';
import { ReservationService } from '@domain/reservation/data-access/reservation.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe, switchMap, tap, catchError } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { SeatStatus } from '@core/constants/enum';
import { SseEvent, SseService } from '@core/services/sse/sse.service';
import { ToastService } from '@core/services/toast/toast.service';
import { errorMessage } from '@shared/utils/error.helper';
import { Store } from '@ngrx/store';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { TranslateService } from '@ngx-translate/core';

export interface SeatState {
  seats: Seat[];

  loading: boolean;
  error: string | null;
}

export const initialState: SeatState = {
  seats: [],
  loading: false,
  error: null,
};

export const SeatStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store, storeNgrx = inject(Store)) => {
    const selectedSeats = computed(() =>
      store.seats().filter((s) => s.status === 'SELECTED'),
    );

    return {
      selectedSeats,
      selectSeatsCount: computed(() => selectedSeats().length),
      totalPrice: computed(() => {
        const price =
          storeNgrx.selectSignal(selectSelectedShowtime)()?.price ?? 0;
        return price * selectedSeats().length;
      }),
      groupedSeats: computed(() => {
        const seats = store.seats();
        const groups = seats.reduce(
          (acc, seat) => {
            const row = seat.seatNumber.charAt(0);
            acc[row] = acc[row] || [];
            acc[row].push(seat);
            return acc;
          },
          {} as Record<string, Seat[]>,
        );

        // Natural sort for row labels: A..Z before AA..ZZ (learned from production Cinema project)
        const sortRowLabels = (a: string, b: string) =>
          a.length === b.length ? a.localeCompare(b) : a.length - b.length;

        return Object.keys(groups)
          .sort(sortRowLabels)
          .map((row) => ({
            row,
            cells: trimOuterEmptyCells(
              buildFixedRow(
                groups[row].map((s) => ({
                  seatNumber: parseInt(s.seatNumber.slice(1)),
                  seatType: s.seatType ?? 'NORMAL',
                  ref: s,
                })),
              ),
            ).map((cell) => ({
              ...cell,
              seats: cell.seats.map((c) => c.ref),
            })),
          }));
      }),
    };
  }),

  withMethods(
    (
      store,
      seatService = inject(SeatService),
      reservationService = inject(ReservationService),
      sseService = inject(SseService),
      translate = inject(TranslateService),
      toast = inject(ToastService),
    ) => {
      // Toggle a whole cell: a merged couple selects/deselects both halves.
      const toggleSeatsImpl = (seats: Seat[]) => {
        const ids = new Set(seats.map((s) => s.id));
        const current = new Map(store.seats().map((s) => [s.id, s]));
        const targets = [...ids]
          .map((id) => current.get(id))
          .filter((s): s is Seat => !!s)
          .filter((s) => s.status !== 'BOOKED' && s.status !== 'HOLD');
        if (targets.length === 0) return;
        const turningOn = targets.filter((s) => s.status !== 'SELECTED').length;
        if (turningOn > 0) {
          const count = store
            .seats()
            .filter((s) => s.status === 'SELECTED').length;
          if (count + turningOn > 8) {
            toast.createNotification({
              type: 'warning',
              title: translate.instant('reservation.maxSeatsTitle'),
              message: translate.instant('reservation.maxSeats'),
              position: 'top',
            });
            return;
          }
        }
        patchState(store, (state) => ({
          seats: state.seats.map((s) =>
            ids.has(s.id) && s.status !== 'BOOKED' && s.status !== 'HOLD'
              ? {
                  ...s,
                  status: (s.status === 'SELECTED'
                    ? 'AVAILABLE'
                    : 'SELECTED') as SeatStatus,
                }
              : s,
          ),
        }));
      };

      return {
      loadSeats: rxMethod<{ showtimeId: number }>(        pipe(
          tap(() =>
            patchState(store, { loading: true, error: null, seats: [] }),
          ),
          switchMap(({ showtimeId }) =>
            seatService.getSeatByShowtime(showtimeId).pipe(
              tapResponse({
                next: (seats) =>
                  patchState(store, { seats, loading: false, error: null }),
                error: (error) => patchState(store, { error: errorMessage(error) }),
              }),
            ),
          ),
        ),
      ),
      //
      selectSeat: (seat: Seat) => {
        patchState(store, (state) => ({
          seats: state.seats.map((s) =>
            s.id === seat.id ? { ...s, status: seat.status } : s,
          ),
          error: null,
        }));
      },
      //
      deselectSeat: (seatId: number) => {
        patchState(store, (state) => ({
          seats: state.seats.map((s) =>
            s.id === seatId ? { ...s, status: 'AVAILABLE' as SeatStatus } : s,
          ),
          error: null,
        }));
      },
      //
      clearSelectedSeat: () =>
        patchState(store, (state) => ({
          seats: state.seats.map((s) =>
            s.status === 'SELECTED'
              ? { ...s, status: 'AVAILABLE' as SeatStatus }
              : s,
          ),
          error: null,
        })),
      // Connected SSE
      initSeatMap: rxMethod<{
        showtimeId: number;
        userId: number;
        reservationId: string | null;
      }>(
        pipe(
          tap(() =>
            patchState(store, { loading: true, error: null, seats: [] }),
          ),
          switchMap(({ showtimeId, userId, reservationId }) =>
            seatService.getSeatByShowtime(showtimeId).pipe(
              tap((seats) =>
                patchState(store, { seats, loading: false }),
              ),
              switchMap(() =>
                reservationService.getLockedSeats(showtimeId).pipe(
                  tap((locked) => {
                    if (!locked.length) return;
                    patchState(store, (state) => ({
                      seats: state.seats.map((s) =>
                        locked.some(
                          (l) =>
                            Number(l.seatId) === s.id &&
                            l.reservationId !== reservationId,
                        ) && s.status === 'AVAILABLE'
                          ? { ...s, status: 'HOLD' as SeatStatus }
                          : s,
                      ),
                    }));
                  }),
                ),
              ),
              switchMap(() =>
                sseService.connect(showtimeId, userId).pipe(
                  tap({
                    next: (res: SseEvent) => {
                      if (
                        res.event === 'SEAT_STATE_INIT' ||
                        res.event === 'SEAT_HOLD'
                      ) {
                        const data = res.data as {
                          seatIds?: { seatId: number; reservationId: string }[];
                        };
                        const holdData = data.seatIds ?? [];
                        const isMyUpdate =
                          holdData.length > 0 &&
                          holdData[0].reservationId === reservationId;
                        const stolen = store
                          .seats()
                          .filter(
                            (s) =>
                              s.status === 'SELECTED' &&
                              holdData.some(
                                (h) =>
                                  Number(h.seatId) === s.id &&
                                  h.reservationId !== reservationId,
                              ),
                          )
                          .map((s) => s.seatNumber);
                        patchState(store, (state) => ({
                          seats: state.seats.map((s) => {
                            const match = holdData.find(
                              (h) => Number(h.seatId) === s.id,
                            );
                            if (match) {
                              return {
                                ...s,
                                status: (match.reservationId === reservationId
                                  ? 'SELECTED'
                                  : 'HOLD') as SeatStatus,
                              };
                            }
                            if (isMyUpdate && s.status === 'SELECTED') {
                              return {
                                ...s,
                                status: 'AVAILABLE' as SeatStatus,
                              };
                            }
                            if (res.event === 'SEAT_STATE_INIT') {
                              return s.status === 'BOOKED'
                                ? s
                                : { ...s, status: 'AVAILABLE' as SeatStatus };
                            }
                            return s;
                          }),
                        }));
                        if (stolen.length > 0) {
                          toast.createNotification({
                            type: 'warning',
                            title: translate.instant('reservation.seatStolenTitle'),
                            message: translate.instant(
                              'reservation.seatStolen',
                              { seats: stolen.join(', ') },
                            ),
                            position: 'top',
                          });
                        }
                      } else if (res.event === 'SEAT_RELEASE') {
                        const releaseIds = (
                          res.data as { seatIds?: number[] }
                        ).seatIds ?? [];
                        patchState(store, (state) => ({
                          seats: state.seats.map((s) =>
                            releaseIds.includes(s.id) && s.status !== 'BOOKED'
                              ? { ...s, status: 'AVAILABLE' as SeatStatus }
                              : s,
                          ),
                        }));
                      }
                    },
                  }),
                  catchError((err) => {
                    console.error('SSE Error:', err);
                    patchState(store, { error: translate.instant('reservation.sseDisconnected') });
                    return EMPTY;
                  }),
                ),
              ),
              catchError((error: unknown) => {
                patchState(store, {
                  error: error instanceof Error ? error.message : String(error),
                  loading: false,
                });
                return EMPTY;
              }),
            ),
          ),
        ),
      ),
      //
      clearError: () => patchState(store, { error: null }),
      //

      toggleSeat(seat: Seat) {
        toggleSeatsImpl([seat]);
      },
      // Toggle a whole cell: a merged couple selects/deselects both halves.
      toggleSeats(seats: Seat[]) {
        toggleSeatsImpl(seats);
      },
      };
    },
  ),
);
