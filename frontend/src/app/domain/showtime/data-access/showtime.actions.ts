import { createActionGroup, emptyProps, props } from '@ngrx/store';
import {
  Showtime,
  ShowtimeRequest,
} from '@domain/showtime/models/showtime.model';
import { FilterDate, Pagination } from '@core/models/common.model';
import { Seat } from '@domain/seat/models/seat.model';

export const ShowtimeActions = createActionGroup({
  source: 'Showtime',
  events: {
    // Load all
    'Load Showtimes By Movie Id': props<{
      movieId: number;
      page: number;
      size: number;
    }>(),
    'Load Showtimes By Movie Id Success': props<{
      showtimes: Showtime[];
      pagination: Pagination;
    }>(),
    'Load Showtimes By Movie Id Failed': props<{ error: unknown }>(),

    'Load Showtimes By Theater Id': props<{
      theaterId: number;
      dateRange?: FilterDate;
      page: number;
      size: number;
    }>(),
    'Load Showtimes By Theater Id Success': props<{
      showtimes: Showtime[];
      pagination: Pagination;
    }>(),
    'Load Showtimes By Theater Id Failed': props<{ error: unknown }>(),

    // Load simple
    'Load Showtime': props<{ id: number }>(),
    'Load Showtime Success': props<{ showtime: Showtime }>(),
    'Load Showtime Failed': props<{ error: unknown }>(),

    // Create
    'Create Showtime': props<{ showtime: ShowtimeRequest }>(),
    'Create Showtime Success': props<{ showtime: Showtime }>(),
    'Create Showtime Failed': props<{ error: unknown }>(),

    // Update
    'Update Showtime': props<{ id: number; showtime: ShowtimeRequest }>(),
    'Update Showtime Success': props<{ showtime: Showtime }>(),
    'Update Showtime Failed': props<{ error: unknown }>(),

    // Delete
    'Delete Showtime': props<{ id: number }>(),
    'Delete Showtime Success': props<{ id: number }>(),
    'Delete Showtime Failed': props<{ error: unknown }>(),

    //   UI Actions
    'Selected Showtime': props<{ showtime: Showtime | null }>(),
    'Clear Error': emptyProps(),

    // Seats
    'Load Seats': props<{ showtimeId: number }>(),
    'Load Seats Success': props<{ seats: Seat[] }>(),
    'Load Seats Failed': props<{ error: unknown }>(),

    'Clear Showtime': emptyProps(),
    'Clear Seats': emptyProps(),
  },
});
