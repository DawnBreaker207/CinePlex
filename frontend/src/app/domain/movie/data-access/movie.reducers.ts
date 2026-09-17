import { createReducer, on } from '@ngrx/store';
import { errorMessage } from '@shared/utils/error.helper';
import { Movie } from '@domain/movie/models/movie.model';
import { Pagination } from '@core/models/common.model';
import { MovieActions } from '@domain/movie/data-access/movie.actions';

export const movieFeatureKey = 'movieKey';

export interface MovieState {
  movies: Movie[];
  selectedMovie: Movie | null;
  pagination: Pagination | null;
  // Loading states
  loading: boolean;
  loadingDetails: boolean;
  saving: boolean;
  searchQuery: string | null;

  error: string | null;
}

export const initialState: MovieState = {
  movies: [],
  selectedMovie: null,
  pagination: null,
  loading: false,
  loadingDetails: false,
  saving: false,
  error: null,
  searchQuery: null,
};

export const movieReducer = createReducer(
  initialState,
  // Load all
  on(MovieActions.loadMovies, (state) => {
    return {
      ...state,
      loading: true,
      error: null,
    };
  }),
  on(MovieActions.loadMoviesSuccess, (state, { movies, pagination }) => {
    return {
      ...state,
      movies: movies,
      pagination: pagination,
      loading: false,
      error: null,
    };
  }),
  on(MovieActions.loadMoviesFailed, (state, { error }) => {
    return {
      ...state,
      loading: false,
      error: errorMessage(error),
    };
  }),
  //   Search
  on(MovieActions.searchMovies, (state, { search }) => {
    return {
      ...state,
      loading: true,
      searchQuery: search,
      error: null,
    };
  }),
  on(MovieActions.searchMoviesSuccess, (state, { movies, pagination }) => {
    return {
      ...state,
      movies: movies,
      pagination: pagination,
      loading: false,
      error: null,
    };
  }),
  on(MovieActions.searchMoviesFailed, (state, { error }) => {
    return {
      ...state,
      loading: false,
      error: errorMessage(error),
    };
  }),
  //  Get one
  on(MovieActions.loadMovie, (state) => {
    return {
      ...state,
      loadingDetails: true,
      error: null,
    };
  }),
  on(MovieActions.loadMovieSuccess, (state, { movie }) => {
    const existingIndex = state.movies.findIndex((m) => m.id === movie.id);
    const movies =
      existingIndex >= 0
        ? state.movies.map((m) => (m.id === movie.id ? movie : m))
        : [...state.movies, movie];
    return {
      ...state,
      movies,
      selectedMovie: movie,
      loadingDetails: false,
      error: null,
    };
  }),
  on(MovieActions.loadMovieFailed, (state, { error }) => {
    return {
      ...state,
      loadingDetails: false,
      error: errorMessage(error),
    };
  }),
  //  Create
  on(MovieActions.createMovie, (state) => {
    return {
      ...state,
      saving: true,
      error: null,
    };
  }),
  on(MovieActions.createMovieSuccess, (state, { movie }) => {
    return {
      ...state,
      movies: [...state.movies, movie],
      saving: false,
      error: null,
    };
  }),
  on(MovieActions.createMovieFailed, (state, { error }) => {
    return {
      ...state,
      saving: false,
      error: errorMessage(error),
    };
  }),
  //  Update
  on(MovieActions.updateMovie, (state) => {
    return {
      ...state,
      saving: true,
      error: null,
    };
  }),
  on(MovieActions.updateMovieSuccess, (state, { movie }) => {
    return {
      ...state,
      movies: state.movies.map((m) => (m.id === movie.id ? movie : m)),
      selectedMovie:
        state.selectedMovie?.id === movie.id ? movie : state.selectedMovie,
      saving: false,
      error: null,
    };
  }),
  on(MovieActions.updateMovieFailed, (state, { error }) => {
    return {
      ...state,
      saving: false,
      error: errorMessage(error),
    };
  }),
  //  Delete
  on(MovieActions.deleteMovie, (state) => {
    return {
      ...state,
      loading: true,
      error: null,
    };
  }),
  on(MovieActions.deleteMovieSuccess, (state, { id }) => {
    return {
      ...state,
      movies: state.movies.filter((m) => m.id !== id),
      selectedMovie:
        state.selectedMovie?.id === id ? null : state.selectedMovie,
      loading: false,
      error: null,
    };
  }),
  on(MovieActions.deleteMovieFailed, (state, { error }) => {
    return {
      ...state,
      loading: false,
      error: errorMessage(error),
    };
  }),
  on(MovieActions.selectedMovie, (state, { movie }) => {
    return {
      ...state,
      selectedMovie: movie,
      error: null,
    };
  }),
  on(MovieActions.clearSelectedMovie, (state) => {
    return {
      ...state,
      selectedMovie: null,
      error: null,
    };
  }),
  on(MovieActions.clearError, (state) => {
    return {
      ...state,
      error: null,
    };
  }),
);
