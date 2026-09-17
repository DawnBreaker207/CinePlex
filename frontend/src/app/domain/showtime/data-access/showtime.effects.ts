import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, of, switchMap } from 'rxjs';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { ShowtimeService } from '@domain/showtime/data-access/showtime.service';

@Injectable()
export class ShowtimeEffects {
  private actions$ = inject(Actions);
  private showtimeService = inject(ShowtimeService);

  loadShowtimesByMovieId$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ShowtimeActions.loadShowtimesByMovieId),
      switchMap(({ movieId, page, size }) =>
        this.showtimeService.getShowtimeByMovie(movieId, { page, size }).pipe(
          map(({ content, pagination }) =>
            ShowtimeActions.loadShowtimesByMovieIdSuccess({
              showtimes: content,
              pagination: pagination,
            }),
          ),
          catchError((err) =>
            of(ShowtimeActions.loadShowtimesByMovieIdFailed({ error: err })),
          ),
        ),
      ),
    );
  });

  loadShowtimesByTheaterId$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ShowtimeActions.loadShowtimesByTheaterId),
      switchMap(({ theaterId, dateRange, page, size }) =>
        this.showtimeService
          .getShowtimeByTheater(theaterId, {
            page,
            size,
            startDate: dateRange?.startDate,
            endDate: dateRange?.endDate,
          })
          .pipe(
            map(({ content, pagination }) =>
              ShowtimeActions.loadShowtimesByTheaterIdSuccess({
                showtimes: content,
                pagination: pagination,
              }),
            ),
            catchError((err) =>
              of(
                ShowtimeActions.loadShowtimesByTheaterIdFailed({ error: err }),
              ),
            ),
          ),
      ),
    );
  });

  loadShowtime$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ShowtimeActions.loadShowtime),
      switchMap(({ id }) =>
        this.showtimeService.getShowtime(id).pipe(
          map((showtime) => ShowtimeActions.loadShowtimeSuccess({ showtime })),
          catchError((err) =>
            of(ShowtimeActions.loadShowtimeFailed({ error: err })),
          ),
        ),
      ),
    );
  });
  createShowtime$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ShowtimeActions.createShowtime),
      switchMap(({ showtime }) =>
        this.showtimeService.createShowtime(showtime).pipe(
          map((showtime) =>
            ShowtimeActions.createShowtimeSuccess({ showtime }),
          ),
          catchError((err) =>
            of(ShowtimeActions.createShowtimeFailed({ error: err })),
          ),
        ),
      ),
    );
  });
  updateShowtime$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ShowtimeActions.updateShowtime),
      switchMap(({ id, showtime }) =>
        this.showtimeService.updateShowtime(id, showtime).pipe(
          map((showtime) =>
            ShowtimeActions.updateShowtimeSuccess({ showtime }),
          ),
          catchError((err) =>
            of(ShowtimeActions.updateShowtimeFailed({ error: err })),
          ),
        ),
      ),
    );
  });
  deleteShowtime$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ShowtimeActions.deleteShowtime),
      switchMap(({ id }) =>
        this.showtimeService.deleteShowtime(id).pipe(
          map(() => ShowtimeActions.deleteShowtimeSuccess({ id })),
          catchError((err) =>
            of(ShowtimeActions.deleteShowtimeFailed({ error: err })),
          ),
        ),
      ),
    );
  });
}

