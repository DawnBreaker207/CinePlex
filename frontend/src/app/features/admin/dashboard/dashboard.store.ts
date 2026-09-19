import {
  DashboardMetrics,
  DashboardQuery,
  DailyForecast,
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
import { Operation } from '@domain/dashboard/models/dashboard.model';
import { DashboardService } from '@domain/dashboard/data-access/dashboard.service';
import { SocketService } from '@core/services/socket/socket.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, finalize, of, pipe, switchMap, tap } from 'rxjs';
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
  forecast: DailyForecast[];
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
  forecast: [],
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
      dashboardService = inject(DashboardService),
      socketService = inject(SocketService),
      translate = inject(TranslateService),
    ) => {
      const applySummary = (summary: {
        metrics: DashboardMetrics;
        revenues: RevenuePoint[];
        movies: TopMovie[];
        theaters: TopTheater[];
        payments: PaymentDistribution[];
      }) =>
        patchState(store, (s) => ({
          previousMetrics: s.metrics,
          metrics: summary.metrics,
          revenues: summary.revenues ?? [],
          movies: summary.movies ?? [],
          theaters: summary.theaters ?? [],
          payments: summary.payments ?? [],
        }));

      const loadSummary = rxMethod<void>(
        pipe(
          tap(() => patchState(store, { isLoading: true, error: null })),
          switchMap(() =>
            dashboardService.getSummary(store.getFilterPayload()).pipe(
              tap((summary) => {
                applySummary(summary);
                patchState(store, { operations: [] });
              }),
              catchError(() => {
                patchState(store, { error: translate.instant('dashboard.loadFailed') });
                return of(null);
              }),
              finalize(() => patchState(store, { isLoading: false })),
            ),
          ),
        ),
      );

      const loadForecast = rxMethod<void>(
        pipe(
          switchMap(() =>
            dashboardService.getForecast(7).pipe(
              tap((forecast) => patchState(store, { forecast })),
              catchError(() => of(null)),
            ),
          ),
        ),
      );

      return {
        setError: (message: string) => patchState(store, { error: message }),
        clearError: () => patchState(store, { error: null }),
        updateFilter: <K extends keyof DashboardState>(key: K, value: DashboardState[K]) => {
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
        loadForecast,
        listenRealtime: rxMethod<void>(
          pipe(
            switchMap(() =>
              socketService.watchEvent<{ action: string }>('/topic/dashboard/update'),
            ),
            tap((data: { action: string }) => {
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
      store.loadForecast();
      store.listenRealtime();
    },
  }),
);
