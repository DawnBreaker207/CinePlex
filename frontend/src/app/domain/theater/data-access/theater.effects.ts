import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { TheaterService } from '@domain/theater/data-access/theater.service';
import { StorageService } from '@core/services/storage/storage.service';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';

@Injectable()
export class TheaterEffects {
  private actions$ = inject(Actions);
  private theaterService = inject(TheaterService);
  private storageService = inject(StorageService);

  loadTheaters$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(TheaterActions.loadTheaters),
      switchMap(({ page, size }) =>
        this.theaterService.getTheaters({ page, size }).pipe(
          map(({ content, pagination }) =>
            TheaterActions.loadTheatersSuccess({
              theaters: content,
              pagination: pagination,
            }),
          ),
          catchError((err) =>
            of(TheaterActions.loadTheatersFailed({ error: err })),
          ),
        ),
      ),
    );
  });

  loadTheater$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(TheaterActions.loadTheater),
      switchMap(({ id }) =>
        this.theaterService.getTheater(id).pipe(
          map((theater) => TheaterActions.loadTheaterSuccess({ theater })),
          catchError((err) =>
            of(TheaterActions.loadTheaterFailed({ error: err })),
          ),
        ),
      ),
    );
  });
  createTheater$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(TheaterActions.createTheater),
      switchMap(({ theater }) =>
        this.theaterService.createTheater(theater).pipe(
          map((theater) => TheaterActions.createTheaterSuccess({ theater })),
          catchError((err) =>
            of(TheaterActions.createTheaterFailed({ error: err })),
          ),
        ),
      ),
    );
  });
  updateTheater$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(TheaterActions.updateTheater),
      switchMap(({ id, theater }) =>
        this.theaterService.updateTheater(id, theater).pipe(
          map((theater) => TheaterActions.updateTheaterSuccess({ theater })),
          catchError((err) =>
            of(TheaterActions.updateTheaterFailed({ error: err })),
          ),
        ),
      ),
    );
  });
  deleteTheater$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(TheaterActions.deleteTheater),
      switchMap(({ id }) =>
        this.theaterService.deleteTheater(id).pipe(
          map(() => TheaterActions.deleteTheaterSuccess({ id })),
          catchError((err) =>
            of(TheaterActions.deleteTheaterFailed({ error: err })),
          ),
        ),
      ),
    );
  });

  setSelectedTheaterId$ = createEffect(
    () => {
      return this.actions$.pipe(
        ofType(TheaterActions.setSelectedTheaterId),
        tap(({ theaterId }) => {
          if (theaterId !== null) {
            this.storageService.setSelectedTheaterId(theaterId);
          } else {
            this.storageService.clearSelectedTheaterId();
          }
        }),
      );
    },
    { dispatch: false },
  );
}

