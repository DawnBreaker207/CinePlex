import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { catchError, map, of, switchMap, tap, withLatestFrom, EMPTY } from 'rxjs';
import { TheaterService } from '@domain/theater/data-access/theater.service';
import { StorageService } from '@core/services/storage/storage.service';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { selectSelectedTheaterId } from '@domain/theater/data-access/theater.selectors';

@Injectable()
export class TheaterEffects {
  private actions$ = inject(Actions);
  private theaterService = inject(TheaterService);
  private storageService = inject(StorageService);
  private store = inject(Store);

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

  // Restore selection after F5 / deep-link: prefer saved theater, else first.
  // Runs once on loadTheatersSuccess (dispatched at boot).
  restoreSelection$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(TheaterActions.loadTheatersSuccess),
      withLatestFrom(this.store.select(selectSelectedTheaterId)),
      switchMap(([{ theaters }, selectedId]) => {
        if (selectedId || theaters.length === 0) return EMPTY;
        const saved = this.storageService.getSelectedTheaterId();
        const id =
          saved != null && theaters.some((t) => t.id === saved)
            ? saved
            : theaters[0].id;
        return of(TheaterActions.setSelectedTheaterId({ theaterId: id }));
      }),
    );
  });
}

