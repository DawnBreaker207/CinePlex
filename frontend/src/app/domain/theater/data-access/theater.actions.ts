import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { Theater, TheaterRequest } from '@domain/theater/models/theater.model';
import { Pagination } from '@core/models/common.model';

export const TheaterActions = createActionGroup({
  source: 'Schedule',
  events: {
    // Load all
    'Load Theaters': props<{ page: number; size: number }>(),
    'Load Theaters Success': props<{
      theaters: Theater[];
      pagination: Pagination;
    }>(),
    'Load Theaters Failed': props<{ error: unknown }>(),

    // Load simple
    'Load Theater': props<{ id: number }>(),
    'Load Theater Success': props<{ theater: Theater }>(),
    'Load Theater Failed': props<{ error: unknown }>(),

    // Create
    'Create Theater': props<{ theater: TheaterRequest }>(),
    'Create Theater Success': props<{ theater: Theater }>(),
    'Create Theater Failed': props<{ error: unknown }>(),

    // Update
    'Update Theater': props<{ id: number; theater: Theater }>(),
    'Update Theater Success': props<{ theater: Theater }>(),
    'Update Theater Failed': props<{ error: unknown }>(),

    // Delete
    'Delete Theater': props<{ id: number }>(),
    'Delete Theater Success': props<{ id: number }>(),
    'Delete Theater Failed': props<{ error: unknown }>(),

    //   UI Actions
    'Set Selected Theater Id': props<{ theaterId: number | null }>(),
    'Selected Theater': props<{ theater: Theater | null }>(),
    'Clear Error': emptyProps(),
    'Clear Selected Theater': emptyProps(),
  },
});
