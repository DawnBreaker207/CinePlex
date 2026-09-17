import { createReducer, on } from '@ngrx/store';
import { errorMessage } from '@shared/utils/error.helper';
import { Showtime } from '@domain/showtime/models/showtime.model';
import { Pagination } from '@core/models/common.model';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';

export const showtimeFeatureKey = 'showtimeKey';

export interface ShowtimeState {
  showtimes: Showtime[];
  selectedShowtime: Showtime | null;
  pagination: Pagination | null;
  movieId: number | null;
  theaterId: number | null;
  loading: boolean;
  loadingDetails: boolean;
  saving: boolean;
  error: string | null;
}

export const initialState: ShowtimeState = {
  showtimes: [],
  selectedShowtime: null,
  pagination: null,
  movieId: null,
  theaterId: null,
  loading: false,
  loadingDetails: false,
  saving: false,
  error: null,
};

export const showtimeReducer = createReducer(
  initialState,
  // Load all
  on(ShowtimeActions.loadShowtimesByMovieId, (state, { movieId }) => {
    return {
      ...state,
      movieId: movieId,
      showtimes: [],
      loading: true,
      error: null,
    };
  }),
  on(ShowtimeActions.loadShowtimesByTheaterId, (state, { theaterId }) => {
    return {
      ...state,
      theaterId: theaterId,
      showtimes: [],
      loading: true,
      error: null,
    };
  }),
  on(
    ShowtimeActions.loadShowtimesByMovieIdSuccess,
    ShowtimeActions.loadShowtimesByTheaterIdSuccess,
    (state, { showtimes, pagination }) => {
      return {
        ...state,
        showtimes: showtimes,
        pagination: pagination,
        loading: false,
        error: null,
      };
    },
  ),
  on(
    ShowtimeActions.loadShowtimesByMovieIdFailed,
    ShowtimeActions.loadShowtimesByTheaterIdFailed,
    (state, { error }) => {
      return {
        ...state,
        loading: false,
        error: errorMessage(error),
      };
    },
  ),
  //  Get one
  on(ShowtimeActions.loadShowtime, (state) => {
    return {
      ...state,
      loadingDetails: true,
      error: null,
    };
  }),
  on(ShowtimeActions.loadShowtimeSuccess, (state, { showtime }) => {
    const existingIndex = state.showtimes.findIndex(
      (s) => s.id === showtime.id,
    );
    const showtimes =
      existingIndex >= 0
        ? state.showtimes.map((s) => (s.id === showtime.id ? showtime : s))
        : [...state.showtimes, showtime];
    return {
      ...state,
      showtimes: showtimes,
      selectedShowtime: showtime,
      loadingDetails: false,
      error: null,
    };
  }),
  on(ShowtimeActions.loadShowtimeFailed, (state, { error }) => {
    return {
      ...state,
      loadingDetails: false,
      error: errorMessage(error),
    };
  }),
  //  Create
  on(ShowtimeActions.createShowtime, (state) => {
    return {
      ...state,
      saving: true,
      error: null,
    };
  }),
  on(ShowtimeActions.createShowtimeSuccess, (state, { showtime }) => {
    return {
      ...state,
      showtimes: [...state.showtimes, showtime],
      saving: false,
      error: null,
    };
  }),
  on(ShowtimeActions.createShowtimeFailed, (state, { error }) => {
    return {
      ...state,
      saving: false,
      error: errorMessage(error),
    };
  }),
  //  Update
  on(ShowtimeActions.updateShowtime, (state) => {
    return {
      ...state,
      saving: true,
      error: null,
    };
  }),
  on(ShowtimeActions.updateShowtimeSuccess, (state, { showtime }) => {
    return {
      ...state,
      showtimes: state.showtimes.map((s) =>
        s.id === showtime.id ? showtime : s,
      ),
      selectedShowtime:
        state.selectedShowtime?.id === showtime.id
          ? showtime
          : state.selectedShowtime,
      saving: false,
      error: null,
    };
  }),
  on(ShowtimeActions.updateShowtimeFailed, (state, { error }) => {
    return {
      ...state,
      saving: false,
      error: errorMessage(error),
    };
  }),
  //  Delete
  on(ShowtimeActions.deleteShowtime, (state) => {
    return {
      ...state,
      loading: true,
      error: null,
    };
  }),
  on(ShowtimeActions.deleteShowtimeSuccess, (state, { id }) => {
    return {
      ...state,
      showtimes: state.showtimes.filter((s) => s.id !== id),
      selectedShowtime:
        state.selectedShowtime?.id === id ? null : state.selectedShowtime,
      loading: false,
      error: null,
    };
  }),
  on(ShowtimeActions.deleteShowtimeFailed, (state, { error }) => {
    return {
      ...state,
      loading: false,
      error: errorMessage(error),
    };
  }),
  on(ShowtimeActions.selectedShowtime, (state, { showtime }) => {
    return {
      ...state,
      selectedShowtime: showtime,
      error: null,
    };
  }),
  on(ShowtimeActions.clearShowtime, (state) => {
    return {
      ...state,
      selectedShowtime: null,
      error: null,
    };
  }),
  on(ShowtimeActions.clearError, (state) => {
    return {
      ...state,
      error: null,
    };
  }),
);
