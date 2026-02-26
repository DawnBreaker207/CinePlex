import { Seat } from '@domain/seat/models/seat.model';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { SeatService } from '@domain/seat/data-access/seat.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { SeatStatus } from '@core/constants/enum';
import { SseService } from '@core/services/sse/sse.service';
import { Store } from '@ngrx/store';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';

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

        return Object.keys(groups)
          .sort()
          .map((row) =>
            groups[row].sort(
              (a, b) =>
                parseInt(a.seatNumber.slice(1)) -
                parseInt(b.seatNumber.slice(1)),
            ),
          );
      }),
    };
  }),

  withMethods(
    (
      store,
      seatService = inject(SeatService),
      sseService = inject(SseService),
    ) => ({
      loadSeats: rxMethod<{ showtimeId: number }>(
        pipe(
          tap(() =>
            patchState(store, { loading: true, error: null, seats: [] }),
          ),
          switchMap(({ showtimeId }) =>
            seatService.getSeatByShowtime(showtimeId).pipe(
              tapResponse({
                next: (seats) =>
                  patchState(store, { seats, loading: false, error: null }),
                error: (error: any) => patchState(store, { error }),
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
              tapResponse({
                next: (seats) => {
                  patchState(store, { seats, loading: false });

                  sseService.connect(showtimeId, userId).subscribe({
                    next: (res: any) => {
                      if (
                        res.event === 'SEAT_STATE_INIT' ||
                        res.event === 'SEAT_HOLD'
                      ) {
                        const holdData = res.data.seatIds || [];

                        const isMyUpdate =
                          holdData.length > 0 &&
                          holdData[0].reservationId === reservationId;
                        patchState(store, (state) => ({
                          seats: state.seats.map((s) => {
                            const match = holdData.find(
                              (h: any) => Number(h.seatId) === s.id,
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
                      } else if (res.event === 'SEAT_RELEASE') {
                        const releaseIds = res.data.seatIds;
                        patchState(store, (state) => ({
                          seats: state.seats.map((s) =>
                            releaseIds.includes(s.id) && s.status !== 'BOOKED'
                              ? { ...s, status: 'AVAILABLE' as SeatStatus }
                              : s,
                          ),
                        }));
                      }
                    },
                    error: (err) => console.error('SSE Error:', err),
                  });
                },
                error: (error: any) =>
                  patchState(store, { error, loading: false }),
              }),
            ),
          ),
        ),
      ),
      //
      toggleSeat(seat: Seat) {
        if (seat.status === 'BOOKED' || seat.status === 'HOLD') return;
        patchState(store, (state) => ({
          seats: state.seats.map((s) =>
            s.id === seat.id
              ? {
                  ...s,
                  status: (s.status === 'SELECTED'
                    ? 'AVAILABLE'
                    : 'SELECTED') as SeatStatus,
                }
              : s,
          ),
        }));
      },
    }),
  ),
);
