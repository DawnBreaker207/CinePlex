import {
  Reservation,
  ReservationFilter,
  ReservationProfile,
} from '@domain/reservation/models/reservation.model';
import { ReservationStatus } from '@core/constants/enum';
import { Pagination } from '@core/models/common.model';
import { errorMessage } from '@shared/utils/error.helper';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { inject } from '@angular/core';
import { ReservationService } from '@domain/reservation/data-access/reservation.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';

export interface ReservationState {
  reservations: Reservation[];
  reservation: Reservation | null;
  pagination: Pagination | null;
  reservationProfile: ReservationProfile[];
  loading: boolean;
  error: string | null;
}

export const initialState: ReservationState = {
  reservations: [],
  reservation: null,
  pagination: null,
  reservationProfile: [],
  loading: false,
  error: null,
};

export const ReservationStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((store, reservationService = inject(ReservationService)) => {
    return {
      loadReservations: rxMethod<{ filter: Partial<ReservationFilter> }>(
        pipe(
          tap(() => patchState(store, { loading: true, error: null })),
          switchMap(({ filter }) =>
            reservationService.getReservations(filter).pipe(
              tapResponse({
                next: ({ content, pagination }) =>
                  patchState(store, {
                    reservations: content,
                    pagination: pagination,
                    loading: false,
                  }),
                error: (err) =>
                  patchState(store, { error: errorMessage(err), loading: false }),
              }),
            ),
          ),
        ),
      ),
      //
      loadReservation: rxMethod<{ id: string }>(
        pipe(
          tap(() => patchState(store, { loading: true, error: null })),
          switchMap(({ id }) =>
            reservationService.getReservation(id).pipe(
              tapResponse({
                next: (reservation) =>
                  patchState(store, {
                    reservation: reservation,
                    loading: false,
                  }),
                error: (err) =>
                  patchState(store, { error: errorMessage(err), loading: false }),
              }),
            ),
          ),
        ),
      ),
      loadReservationProfile: rxMethod<{ id: number; status: ReservationStatus }>(
        pipe(
          tap(() => patchState(store, { loading: true, error: null })),
          switchMap(({ id, status }) =>
            reservationService.getUserReservation(id, status).pipe(
              tapResponse({
                next: ({ content }) =>
                  patchState(store, {
                    reservationProfile: content,
                    loading: false,
                  }),
                error: (err) =>
                  patchState(store, { error: errorMessage(err), loading: false }),
              }),
            ),
          ),
        ),
      ),
    };
  }),
);
