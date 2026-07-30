import { Component, computed, effect, inject, input, linkedSignal, numberAttribute, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { DatePipe, DOCUMENT, NgClass, NgOptimizedImage } from '@angular/common';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzRateModule } from 'ng-zorro-antd/rate';
import { FormsModule } from '@angular/forms';
import { selectMovieById } from '@domain/movie/data-access/movie.selectors';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { selectAllShowtimes } from '@domain/showtime/data-access/showtime.selectors';
import {
  selectAllTheaters,
  selectSelectedTheaterId,
} from '@domain/theater/data-access/theater.selectors';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { selectUserId, selectIsAuthenticated } from '@core/auth/auth.selectors';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import dayjs from 'dayjs';
import { TranslatePipe } from '@ngx-translate/core';

interface Review {
  user: string;
  avatar: string;
  rating: number;
  comment: string;
  date: string;
}

@Component({
  selector: 'app-detail',
  imports: [
    DatePipe,
    NzTagModule,
    NzButtonModule,
    NzIconModule,
    NzTabsModule,
    NzAvatarModule,
    NzRateModule,
    FormsModule,
    LoadingComponent,
    RouterLink,
    NzSkeletonModule,
    NgClass,
    NgOptimizedImage,
    TranslatePipe,
  ],
  templateUrl: './detail.component.html',
})
export class DetailComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);
  private reservationStore = inject(ReservationStore);
  private document = inject(DOCUMENT);

  id = input(0, { transform: numberAttribute });
  selectedDate = linkedSignal({
    source: this.id,
    computation: () => dayjs().startOf('day').toDate(),
  });
  showtimesLoading = signal(true);

  isAuthenticated = this.store.selectSignal(selectIsAuthenticated);
  userId = this.store.selectSignal(selectUserId);

  movie = computed(() => {
    const id = this.id();
    return id ? this.store.selectSignal(selectMovieById(id))() : null;
  });

  allShowtimes = this.store.selectSignal(selectAllShowtimes);
  allTheaters = this.store.selectSignal(selectAllTheaters);
  selectedTheaterId = this.store.selectSignal(selectSelectedTheaterId);

  dates = computed(() => {
    return Array.from({ length: 7 }).map((_, i) =>
      dayjs().add(i, 'day').toDate(),
    );
  });

  showtimes = computed(() => {
    const movie = this.movie();
    const showtimes = this.allShowtimes();
    const theaters = this.allTheaters();
    const currentTheaterId = this.selectedTheaterId();
    const date = this.selectedDate();

    if (!movie || !showtimes.length || !currentTheaterId) return [];

    const filtered = showtimes.filter(
      (st) =>
        st.movieId === movie.id &&
        dayjs(st.showDate).isSame(date, 'day') &&
        st.theaterId === currentTheaterId,
    );
    if (filtered.length === 0) return [];

    const groupTheater = new Map<number, any>();

    filtered.forEach((st) => {
      const theater = theaters.find((t) => t.id === st.theaterId);
      if (theater) {
        if (!groupTheater.has(theater.id)) {
          groupTheater.set(theater.id, {
            id: theater.id,
            name: theater.name,
            showtimes: [],
          });
        }
        groupTheater.get(theater.id).showtimes.push({
          id: st.id,
          time: st.showTime.toString().substring(0, 5),
          availableSeats: st.availableSeats,
          totalSeats: st.totalSeats || 100,
        });
      }
    });
    return Array.from(groupTheater.values());
  });

  constructor() {
    effect(() => {
      if (this.allShowtimes().length > 0) {
        this.showtimesLoading.set(false);
      }
    });
  }

  ngOnInit() {
    const id = this.id();
    if (id) {
      this.showtimesLoading.set(true);
      this.store.dispatch(MovieActions.loadMovie({ id: id }));
      this.store.dispatch(
        ShowtimeActions.loadShowtimesByMovieId({
          movieId: id,
          page: 0,
          size: 50,
        }),
      );
      if (this.allTheaters().length === 0) {
        this.store.dispatch(
          TheaterActions.loadTheaters({ page: 0, size: 100 }),
        );
      }
    }
  }

  selectDate(date: Date) {
    this.selectedDate.set(date);
  }

  scrollToShowtimes() {
    this.document.getElementById('showtime-section')?.scrollIntoView({ behavior: 'smooth' });
  }

  bookShowtime(showtimeId: number) {
    if (!this.isAuthenticated()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    const theaterId = this.selectedTheaterId();
    const uid = this.userId();
    if (!theaterId || !uid) return;
    this.reservationStore.initReservation({
      reservationId: '',
      userId: uid,
      showtimeId: showtimeId,
      theaterId: theaterId,
    });
  }

  reviews: Review[] = [];
}
