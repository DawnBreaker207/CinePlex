import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import {
  catchError,
  concat,
  distinctUntilChanged,
  EMPTY,
  filter,
  from,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { MovieService } from '@domain/movie/data-access/movie.service';
import { StorageService } from '@core/services/storage/storage.service';

@Injectable()
export class MovieEffects {
  private actions$ = inject(Actions);
  private movieService = inject(MovieService);
  private storage = inject(StorageService);
  loadMovies$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(MovieActions.loadMovies),
      switchMap(({ page, size }) => {
        const cache$ = from(this.storage.getMovies(page, size)).pipe(
          filter((res) => res.content.length > 0),
        );

        const network$ = this.movieService.getMovieLists({ page, size }).pipe(
          tap(({ content }) => this.storage.cacheMovie(content)),
          catchError((err) => {
            console.error('Network error ', err);
            return EMPTY;
          }),
        );

        return concat(cache$, network$).pipe(
          distinctUntilChanged((prev, curr) => {
            return (
              JSON.stringify(prev.content) === JSON.stringify(curr.content) &&
              prev.pagination.totalElements === curr.pagination.totalElements
            );
          }),
          map(({ content, pagination }) =>
            MovieActions.loadMoviesSuccess({
              movies: content,
              pagination: pagination,
            }),
          ),
          catchError((err) =>
            of(MovieActions.loadMoviesFailed({ error: err })),
          ),
        );
      }),
    );
  });

  searchMovies$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(MovieActions.searchMovies),
      switchMap(({ search }) =>
        this.movieService.getMovieLists(search).pipe(
          map(({ content, pagination }) =>
            MovieActions.searchMoviesSuccess({
              movies: content,
              pagination: pagination,
            }),
          ),
          catchError((err) =>
            of(MovieActions.searchMoviesFailed({ error: err })),
          ),
        ),
      ),
    );
  });

  loadMovie$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(MovieActions.loadMovie),
      switchMap(({ id }) =>
        this.movieService.findOneMovie(id).pipe(
          map((movie) => MovieActions.loadMovieSuccess({ movie })),
          catchError((err) => of(MovieActions.loadMovieFailed({ error: err }))),
        ),
      ),
    );
  });

  createMovie$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(MovieActions.createMovie),
      switchMap(({ movie }) =>
        this.movieService.saveMovie(movie).pipe(
          map((movie) => MovieActions.createMovieSuccess({ movie })),
          catchError((err) =>
            of(MovieActions.createMovieFailed({ error: err })),
          ),
        ),
      ),
    );
  });

  updateMovie$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(MovieActions.updateMovie),
      switchMap(({ id, movie }) =>
        this.movieService.updateMovie(id, movie).pipe(
          map((movie) => MovieActions.updateMovieSuccess({ movie })),
          catchError((err) =>
            of(MovieActions.updateMovieFailed({ error: err })),
          ),
        ),
      ),
    );
  });

  deleteMovie$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(MovieActions.deleteMovie),
      switchMap(({ id }) =>
        this.movieService.removeMovie(id).pipe(
          map(() => MovieActions.deleteMovieSuccess({ id })),
          catchError((err) =>
            of(MovieActions.deleteMovieFailed({ error: err })),
          ),
        ),
      ),
    );
  });
}
