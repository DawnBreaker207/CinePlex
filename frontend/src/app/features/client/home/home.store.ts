import { Theater } from '@domain/theater/models/theater.model';
import { Movie } from '@domain/movie/models/movie.model';
import { signalStore, withMethods } from '@ngrx/signals';
import { computed, inject, signal, effect, untracked, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import dayjs from 'dayjs';
import { NzModalService } from 'ng-zorro-antd/modal';
import { StorageService } from '@core/services/storage/storage.service';
import {
  selectAllTheaters,
  selectSelectedTheaterId,
} from '@domain/theater/data-access/theater.selectors';
import { selectAllShowtimes } from '@domain/showtime/data-access/showtime.selectors';
import { selectAllMovies } from '@domain/movie/data-access/movie.selectors';
import { selectJwt } from '@core/auth/auth.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { ShowtimeModalComponent } from '@features/client/home/components/showtime-modal/modal.component';
import { ReservationStore } from '@features/client/reservation/reservation.store';

export const HomeStore = signalStore(
  withMethods(
    (
      store,
      ngrxStore = inject(Store),
      router = inject(Router),
      modalService = inject(NzModalService),
      storageService = inject(StorageService),
      reservationStore = inject(ReservationStore),
    ) => {
      const theaters = ngrxStore.selectSignal(selectAllTheaters);
      const showtimes = ngrxStore.selectSignal(selectAllShowtimes);
      const allMovies = ngrxStore.selectSignal(selectAllMovies);
      const selectedTheaterId = ngrxStore.selectSignal(selectSelectedTheaterId);
      const user = ngrxStore.selectSignal(selectJwt);

      const loading = signal(false);
      const error = signal<string | null>(null);

      const showtimesForTheater = computed(() => {
        const id = selectedTheaterId();
        return id ? showtimes().filter((st) => st.theaterId === id) : [];
      });

      const movies = computed(() => {
        const showtimes = showtimesForTheater();
        const movies = allMovies();
        if (!showtimes.length || !movies.length) return [];

        const movieMap = new Map(movies.map((m) => [m.id, m]));
        const uniqueIds = [...new Set(showtimes.map((st) => st.movieId))];
        return uniqueIds
          .map((id) => movieMap.get(id))
          .filter((m): m is Movie => !!m);
      });

      const nowShowing = computed(() =>
        movies().filter((m) => !dayjs(m.releaseDate).isAfter(dayjs(), 'day')),
      );

      const comingSoon = computed(() =>
        movies().filter((m) => dayjs(m.releaseDate).isAfter(dayjs(), 'day')),
      );

      effect(() => {
        if (movies().length > 0) {
          untracked(() => loading.set(false));
        }
      });

      effect((onCleanup) => {
        if (loading()) {
          const timer = setTimeout(() => {
            untracked(() => {
              loading.set(false);
              error.set('Không thể tải dữ liệu. Vui lòng kiểm tra kết nối và thử lại.');
            });
          }, 15000);
          onCleanup(() => clearTimeout(timer));
        }
      });

      return {
        theaters,
        selectedTheaterId,
        movies,
        nowShowing,
        comingSoon,
        showtimesForTheater,
        user,
        loading: loading.asReadonly(),
        error: error.asReadonly(),
        setError(msg: string) { error.set(msg); },
        clearError() { error.set(null); },
        initData() {
          const currentTheaters = theaters();
          const currentMovies = allMovies();

          if (currentTheaters.length > 0 && currentMovies.length > 0) {
            loading.set(false);
          } else {
            loading.set(true);
          }

          if (currentTheaters.length === 0) {
            ngrxStore.dispatch(
              TheaterActions.loadTheaters({ page: 0, size: 10 }),
            );
          }
          if (currentMovies.length === 0) {
            ngrxStore.dispatch(MovieActions.loadMovies({ page: 0, size: 100 }));
          }

          if (currentTheaters.length > 0 && !selectedTheaterId()) {
            const savedTheater = storageService.getSelectedTheaterId();
            const foundTheater = currentTheaters.find(
              (t) => t.id === savedTheater,
            );

            const id = savedTheater && foundTheater ? savedTheater : currentTheaters[0].id;
            ngrxStore.dispatch(TheaterActions.setSelectedTheaterId({ theaterId: id }));
            ngrxStore.dispatch(ShowtimeActions.loadShowtimesByTheaterId({ theaterId: id, page: 0, size: 100 }));
          }
        },

        selectTheater(theaterId: number) {
          ngrxStore.dispatch(
            TheaterActions.setSelectedTheaterId({ theaterId }),
          );
          storageService.setSelectedTheaterId(theaterId);
          ngrxStore.dispatch(
            ShowtimeActions.loadShowtimesByTheaterId({
              theaterId,
              page: 0,
              size: 100,
            }),
          );
        },

        loadShowtimes(theaterId: number) {
          ngrxStore.dispatch(
            ShowtimeActions.loadShowtimesByTheaterId({
              theaterId: theaterId,
              page: 0,
              size: 100,
            }),
          );
        },

        handleMovieSelection(movieId: number) {
          const movieShowtimes = showtimesForTheater().filter(
            (st) => st.movieId === movieId,
          );
          const movie = allMovies().find((m) => m.id === movieId);
          const modal = modalService.create({
            nzTitle: undefined,
            nzContent: ShowtimeModalComponent,
            nzData: { showtimes: movieShowtimes, movieTitle: movie?.title },
            nzFooter: null,
          });
          const destroyRef = inject(DestroyRef);
          modal.afterClose.pipe(takeUntilDestroyed(destroyRef)).subscribe((stId) => {
            if (stId) this.proceedToReservation(stId);
          });
        },

        proceedToReservation(showtimeId: number) {
          const currentUser = user();
          const theaterId = selectedTheaterId();

          if (!currentUser) {
            router.navigate(['/login'], { queryParams: { returnUrl: '/home' } });
            return;
          }
          if (!theaterId) return;
          reservationStore.initReservation({
            reservationId: '',
            userId: currentUser.userId,
            showtimeId: showtimeId,
            theaterId: theaterId,
          });
        },
      };
    },
  ),
);
