import {
  Component,
  computed,
  effect,
  inject,
  input,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { ShowtimeService } from '@domain/showtime/data-access/showtime.service';
import { TheaterService } from '@domain/theater/data-access/theater.service';
import {
  selectSelectedTheater,
  selectSelectedTheaterId,
} from '@domain/theater/data-access/theater.selectors';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { selectJwt } from '@core/auth/auth.selectors';
import { Showtime } from '@domain/showtime/models/showtime.model';
import { Theater } from '@domain/theater/models/theater.model';
import { timeFormat } from '@shared/utils/date.helper';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';
import { finalize } from 'rxjs';
import dayjs from 'dayjs';

@Component({
  selector: 'app-showtimes',
  imports: [
    RouterLink,
    DatePipe,
    NzButtonModule,
    NzEmptyModule,
    NzSkeletonModule,
    NzIconModule,
    TranslatePipe,
    ErrorStateComponent,
  ],
  templateUrl: './showtimes.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowtimesComponent implements OnInit {
  private showtimeService = inject(ShowtimeService);
  private theaterService = inject(TheaterService);
  private reservationStore = inject(ReservationStore);
  private store = inject(Store);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly theaters = signal<Theater[]>([]);
  readonly theaterId = signal<number | null>(null);
  readonly storedTheaterId = this.store.selectSignal(selectSelectedTheaterId);
  readonly currentTheater = this.store.selectSignal(selectSelectedTheater);
  readonly embedded = input(false);
  readonly presetTheaterId = input<number | null>(null);  readonly dates = signal<Date[]>(
    Array.from({ length: 7 }).map((_, i) =>
      dayjs().add(i, 'day').toDate(),
    ),
  );
  readonly selectedDate = signal<Date>(dayjs().startOf('day').toDate());
  readonly showtimes = signal<Showtime[]>([]);
  readonly loading = signal(false);
  readonly loadError = signal(false);

  readonly grouped = computed(() => {
    const map = new Map<number, { title: string; items: Showtime[] }>();
    for (const st of this.showtimes()) {
      const g = map.get(st.movieId) ?? { title: st.movieTitle, items: [] };
      g.items.push(st);
      map.set(st.movieId, g);
    }
    return [...map.entries()].map(([movieId, g]) => ({
      movieId,
      title: g.title,
      poster: g.items[0]?.moviePosterUrl,
      items: [...g.items].sort((a, b) =>
        dayjs(a.showTime).diff(dayjs(b.showTime)),
      ),
    }));
  });

  protected readonly timeFormat = timeFormat;

  // No-op until ngOnInit's own load resolves, so this can't race it.
  private initialized = false;

  constructor() {
    effect(() => {
      const preset = this.presetTheaterId();
      const stored = this.storedTheaterId();
      if (!this.initialized) return;
      if (preset && preset !== this.theaterId()) {
        this.theaterId.set(preset);
        this.load();
        return;
      }
      if (this.embedded()) return;
      if (stored && stored !== this.theaterId()) {
        this.theaterId.set(stored);
        this.load();
      }
    });
  }

  ngOnInit() {
    this.theaterService.getTheaters({ page: 0, size: 100 }).subscribe({
      next: (page) => {
        this.theaters.set(page.content ?? []);
        const preset = this.presetTheaterId();
        const stored = this.storedTheaterId();
        const q = Number(this.route.snapshot.queryParamMap.get('theater'));
        const first =
          this.theaters().find((t) => t.id === (preset ?? stored ?? q)) ??
          this.theaters()[0];
        if (first) {
          this.theaterId.set(first.id);
          this.load();
        }
        this.initialized = true;
      },
    });
  }

  onDateChange(date: Date) {
    this.selectedDate.set(date);
    this.load();
  }

  load() {
    const theaterId = this.theaterId();
    if (!theaterId) return;
    this.loading.set(true);
    this.loadError.set(false);
    this.showtimeService
      .getShowtimeByTheater(theaterId, {
        startDate: dayjs(this.selectedDate()).format('YYYY-MM-DD'),
        endDate: dayjs(this.selectedDate()).format('YYYY-MM-DD'),
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.showtimes.set(page.content ?? []),
        error: () => {
          this.showtimes.set([]);
          this.loadError.set(true);
        },
      });
  }

  book(showtime: Showtime) {
    if (showtime.availableSeats === 0) return;
    const user = this.store.selectSignal(selectJwt)();
    if (!user) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: '/showtimes' },
      });
      return;
    }
    this.reservationStore.initReservation({
      reservationId: '',
      userId: user.userId,
      showtimeId: showtime.id,
      theaterId: showtime.theaterId,
    });
  }
}
