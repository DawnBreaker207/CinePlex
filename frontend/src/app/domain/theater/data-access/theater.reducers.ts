import { createReducer, on } from '@ngrx/store';
import { errorMessage } from '@shared/utils/error.helper';
import { Theater } from '@domain/theater/models/theater.model';
import { Pagination } from '@core/models/common.model';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';

export const theaterFeatureKey = 'theaterKey';

export interface TheaterState {
  theaters: Theater[];
  selectedTheater: Theater | null;
  selectedTheaterId: number | null;
  pagination: Pagination | null;
  loading: boolean;
  loadingDetails: boolean;
  saving: boolean;
  error: string | null;
}

export const initialState: TheaterState = {
  theaters: [],
  selectedTheater: null,
  selectedTheaterId: null,
  pagination: null,
  loading: false,
  loadingDetails: false,
  saving: false,
  error: null,
};

export const theaterReducer = createReducer(
  initialState,
  // Load all
  on(TheaterActions.loadTheaters, (state) => {
    return {
      ...state,
      loading: true,
      error: null,
    };
  }),
  on(TheaterActions.loadTheatersSuccess, (state, { theaters, pagination }) => {
    return {
      ...state,
      theaters: theaters,
      pagination: pagination,
      loading: false,
      error: null,
    };
  }),
  on(TheaterActions.loadTheatersFailed, (state, { error }) => {
    return {
      ...state,
      loading: false,
      error: errorMessage(error),
    };
  }),
  //  Get one
  on(TheaterActions.loadTheater, (state) => {
    return {
      ...state,
      loadingDetails: true,
      error: null,
    };
  }),
  on(TheaterActions.loadTheaterSuccess, (state, { theater }) => {
    const existingIndex = state.theaters.findIndex((t) => t.id === theater.id);
    const theaters =
      existingIndex >= 0
        ? state.theaters.map((t) => (t.id === theater.id ? theater : t))
        : [...state.theaters, theater];
    return {
      ...state,
      theaters: theaters,
      selectedTheater: theater,
      loadingDetails: false,
      error: null,
    };
  }),
  on(TheaterActions.loadTheaterFailed, (state, { error }) => {
    return {
      ...state,
      loadingDetails: false,
      error: errorMessage(error),
    };
  }),
  //  Create
  on(TheaterActions.createTheater, (state) => {
    return {
      ...state,
      saving: true,
      error: null,
    };
  }),
  on(TheaterActions.createTheaterSuccess, (state, { theater }) => {
    return {
      ...state,
      theaters: [...state.theaters, theater],
      saving: false,
      error: null,
    };
  }),
  on(TheaterActions.createTheaterFailed, (state, { error }) => {
    return {
      ...state,
      saving: false,
      error: errorMessage(error),
    };
  }),
  //  Update
  on(TheaterActions.updateTheater, (state) => {
    return {
      ...state,
      saving: true,
      error: null,
    };
  }),
  on(TheaterActions.updateTheaterSuccess, (state, { theater }) => {
    return {
      ...state,
      theaters: state.theaters.map((t) => (t.id === theater.id ? theater : t)),
      selectedTheater:
        state.selectedTheater?.id === theater.id
          ? theater
          : state.selectedTheater,
      saving: false,
      error: null,
    };
  }),
  on(TheaterActions.updateTheaterFailed, (state, { error }) => {
    return {
      ...state,
      saving: false,
      error: errorMessage(error),
    };
  }),
  //  Delete
  on(TheaterActions.deleteTheater, (state) => {
    return {
      ...state,
      loading: true,
      error: null,
    };
  }),
  on(TheaterActions.deleteTheaterSuccess, (state, { id }) => {
    return {
      ...state,
      theaters: state.theaters.filter((t) => t.id !== id),
      selectedTheater:
        state.selectedTheater?.id === id ? null : state.selectedTheater,
      loading: false,
      error: null,
    };
  }),
  on(TheaterActions.deleteTheaterFailed, (state, { error }) => {
    return {
      ...state,
      loading: false,
      error: errorMessage(error),
    };
  }),
  on(TheaterActions.selectedTheater, (state, { theater }) => {
    return {
      ...state,
      selectedTheater: theater,
      error: null,
    };
  }),
  on(TheaterActions.clearSelectedTheater, (state) => {
    return {
      ...state,
      selectedTheater: null,
      error: null,
    };
  }),
  on(TheaterActions.clearError, (state) => {
    return {
      ...state,
      error: null,
    };
  }),

  on(TheaterActions.setSelectedTheaterId, (state, { theaterId }) => ({
    ...state,
    selectedTheaterId: theaterId,
    // Keep id and object in sync: resolve from the loaded list.
    selectedTheater:
      theaterId == null
        ? null
        : (state.theaters.find((t) => t.id === theaterId) ??
          state.selectedTheater),
  })),
);
