import {
  DashboardMetrics,
  DashboardQuery,
  PaymentDistribution,
  RevenuePoint,
  TopMovie,
  TopTheater,
} from '@domain/dashboard/models/dashboard.model';
import {
  patchState,
  signalStore,
  withComputed,
  withHooks,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { getMockSummary, Operation } from '@features/admin/dashboard/mocks/dashboard.mock';
import { SocketService } from '@core/services/socket/socket.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, finalize, from, of, pipe, switchMap, tap } from 'rxjs';
import { formatDate } from '@shared/utils/date.helper';
import { TranslateService } from '@ngx-translate/core';

export interface DashboardState {
  isLoading: boolean;
  error: string | null;
  filterType: 'week' | 'month' | 'quarter' | 'year';
  selectedDate: Date;
  selectedQuarterYear: number;
  selectedQuarter: number;
  metrics: DashboardMetrics | null;
  previousMetrics: DashboardMetrics | null;
  revenues: RevenuePoint[];
  payments: PaymentDistribution[];
  theaters: TopTheater[];
  movies: TopMovie[];
  operations: Operation[];
  isAutoUpdate: boolean;
}

export type FilterType = DashboardState['filterType'];

export function computeFilterRange(
  type: FilterType,
  date: Date,
  quarter: number,
  quarterYear: number,
): { start: Date; end: Date } {
  let start: Date;
  let end: Date;

  const month = date.getMonth();
  const year = date.getFullYear();
  const diff = date.getDay() === 0 ? -6 : 1 - date.getDay();
  const startMonth = (quarter - 1) * 3;

  switch (type) {
    case 'week':
      start = new Date(date);
      start.setDate(start.getDate() + diff);

      end = new Date(start);
      end.setDate(start.getDate() + 6);
      break;
    case 'month':
      start = new Date(year, month, 1);
      end = new Date(year, month + 1, 0);
      break;
    case 'quarter':
      start = new Date(quarterYear, startMonth, 1);
      end = new Date(quarterYear, startMonth + 3, 0);
      break;
    case 'year':
      start = new Date(year, 0, 1);
      end = new Date(year, 11, 31);
      break;
    default:
      start = new Date();
      end = new Date();
  }
  return { start, end };
}

export const initialState: DashboardState = {
  isLoading: false,
  error: null,
  filterType: 'week',
  selectedDate: new Date(),
  selectedQuarter: Math.floor((new Date().getMonth() + 3) / 3),
  selectedQuarterYear: new Date().getFullYear(),
  metrics: null,
  previousMetrics: null,
  revenues: [],
  movies: [],
  theaters: [],
  payments: [],
  operations: [],
  isAutoUpdate: false,
};
export const DashboardStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    getFilterPayload: computed((): DashboardQuery => {
      const date = store.selectedDate();
      const quarter = store.selectedQuarter() || 1;
      const quarterYear = store.selectedQuarterYear() || date.getFullYear();
      const { start, end } = computeFilterRange(
        store.filterType(),
        date,
        quarter,
        quarterYear,
      );
      return {
        startDate: formatDate(start),
        endDate: formatDate(end),
      };
    }),
  })),
  withMethods(
    (
      store,
      socketService = inject(SocketService),
      translate = inject(TranslateService),
    ) => {
      const loadSummary = rxMethod<void>(
        pipe(
          tap(() => patchState(store, { isLoading: true })),
          switchMap(() =>
            from(getMockSummary(store.getFilterPayload())).pipe(
              tap(
                ({
                  metrics,
                  previousMetrics,
                  revenues,
                  movies,
                  theaters,
                  payments,
                  operations,
                }) =>
                  patchState(store, {
                    metrics: metrics,
                    previousMetrics: previousMetrics,
                    revenues: revenues,
                    movies: movies,
                    theaters: theaters,
                    payments: payments,
                    operations: operations,
                  }),
              ),
              catchError((err) => {
                patchState(store, { error: translate.instant('dashboard.loadFailed') });
                return of(null);
              }),
              finalize(() => patchState(store, { isLoading: false })),
            ),
          ),
        ),
      );

      return {
        setError: (message: string) => patchState(store, { error: message }),
        clearError: () => patchState(store, { error: null }),
        updateFilter: (key: keyof DashboardState, value: any) => {
          patchState(store, { [key]: value });
          loadSummary();
        },
        resetFilters: () => {
          patchState(store, {
            filterType: initialState.filterType,
            selectedDate: new Date(),
            selectedQuarter: Math.floor((new Date().getMonth() + 3) / 3),
            selectedQuarterYear: new Date().getFullYear(),
          });
          loadSummary();
        },
        resolveOperation: (id: string) =>
          patchState(store, {
            operations: store.operations().filter((op) => op.id !== id),
          }),
        toggleAutoUpdate: () => {
          const state = !store.isAutoUpdate();
          patchState(store, { isAutoUpdate: state });
          if (state) {
            loadSummary();
          }
        },
        listenRealtime: rxMethod<void>(
          pipe(
            switchMap(() =>
              socketService.watchEvent<any>('/topic/dashboard/update'),
            ),
            tap((data: any) => {
              if (store.isAutoUpdate() && data.action === 'REFRESH') {
                loadSummary();
              }
            }),
          ),
        ),
        loadSummary,
      };
    },
  ),
  withHooks({
    onInit(store) {
      store.loadSummary();
      store.listenRealtime();
    },
  }),
);
