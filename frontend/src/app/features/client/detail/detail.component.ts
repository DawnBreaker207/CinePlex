import {
  Component,
  computed,
  ElementRef,
  inject,
  input,
  linkedSignal,
  numberAttribute,
  OnInit,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { selectJwt } from '@core/auth/auth.selectors';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzRateModule } from 'ng-zorro-antd/rate';
import { FormsModule } from '@angular/forms';
import { selectMovieById } from '@domain/movie/data-access/movie.selectors';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import {
  selectAllShowtimes,
  selectShowtimeLoading,
} from '@domain/showtime/data-access/showtime.selectors';
import {
  selectAllTheaters,
  selectSelectedTheaterId,
} from '@domain/theater/data-access/theater.selectors';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import dayjs from 'dayjs';
import {
  CinemaGroup,
  ShowtimeSectionComponent,
} from '@features/client/detail/components/showtime-section/showtime-section.component';
import { DetailHeaderComponent } from '@features/client/detail/components/detail-header/detail-header.component';

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
    NzTabsModule,
    NzAvatarModule,
    NzRateModule,
    FormsModule,
    LoadingComponent,
    NzSkeletonModule,
    TranslatePipe,
    NzIconModule,
    NzButtonModule,
    NzToolTipModule,
    DetailHeaderComponent,
    ShowtimeSectionComponent,
  ],
  templateUrl: './detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);
  private reservationStore = inject(ReservationStore);

  id = input(0, { transform: numberAttribute });
  selectedDate = linkedSignal({
    source: this.id,
    computation: () => dayjs().startOf('day').toDate(),
  });
  // Real store flag — can't get stuck true when a movie has zero showtimes.
  showtimesLoading = this.store.selectSignal(selectShowtimeLoading);

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

  showtimes = computed<CinemaGroup[]>(() => {
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

    const groupTheater = new Map<number, CinemaGroup>();

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
        groupTheater.get(theater.id)!.showtimes.push({
          id: st.id,
          time: st.showTime.toString().substring(0, 5),
          availableSeats: st.availableSeats ?? -1,
          totalSeats: st.totalSeats || 100,
        });
      }
    });
    return Array.from(groupTheater.values());
  });

  private showtimesSection = viewChild('showtimesSection', { read: ElementRef });

  scrollToShowtimes(): void {
    this.showtimesSection()?.nativeElement?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  }

  bookShowtime(sel: { showtimeId: number; theaterId: number }): void {
    const user = this.store.selectSignal(selectJwt)();
    if (!user) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: `/movie/detail/${this.id()}` },
      });
      return;
    }
    this.reservationStore.initReservation({
      reservationId: '',
      userId: user.userId,
      showtimeId: sel.showtimeId,
      theaterId: sel.theaterId,
    });
  }

  ngOnInit() {
    const id = this.id();
    if (id) {
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

  reviews: Review[] = [];
}
