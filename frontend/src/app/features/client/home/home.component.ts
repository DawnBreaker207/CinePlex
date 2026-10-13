import {
  Component,
  computed,
  effect,
  inject,
  OnInit,
  untracked,
  ChangeDetectionStrategy,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzImageService } from 'ng-zorro-antd/image';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { SliderComponent } from '@features/client/home/components/slider/slider.component';
import { HomeStore } from '@features/client/home/home.store';
import { RecommendationService } from '@domain/movie/data-access/recommendation.service';
import { Movie } from '@domain/movie/models/movie.model';
import { TranslatePipe } from '@ngx-translate/core';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';
import { FormsModule } from '@angular/forms';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { MovieItemComponent } from '@shared/components/movie-item/movie-item.component';

import { NzIconModule } from 'ng-zorro-antd/icon';
import { ShowtimeService } from '@domain/showtime/data-access/showtime.service';
import { Showtime } from '@domain/showtime/models/showtime.model';
import { signal } from '@angular/core';

@Component({
  selector: 'app-client',
  imports: [
    RouterLink,
    FormsModule,
    NzLayoutModule,
    NzGridModule,
    NzTabsModule,
    NzButtonModule,
    NzAlertModule,
    NzSkeletonModule,
    NzModalModule,
    NzSelectModule,
    NzIconModule,
    SliderComponent,
    TranslatePipe,
    MovieItemComponent,
    ErrorStateComponent,
  ],
  providers: [NzImageService, HomeStore],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit {
  readonly homeStore = inject(HomeStore);
  private recommendations = inject(RecommendationService);
  private showtimeService = inject(ShowtimeService);

  readonly suggested = rxResource<Movie[], []>({
    stream: () => this.recommendations.recommend(6),
  });

  readonly suggestedOrFallback = computed(() => {
    const suggested = this.suggested.value() ?? [];
    if (suggested.length > 0) return suggested;
    return (this.homeStore.nowShowing() ?? []).slice(0, 6);
  });

  // 4-STEP QUICK BOOKING RIBBON SIGNALS
  quickMovieId = signal<number | null>(null);
  quickTheaterId = signal<number | null>(null);
  quickDate = signal<string | null>(null);
  quickShowtimeId = signal<number | null>(null);
  quickMovieShowtimes = signal<Showtime[]>([]);
  quickLoadingShowtimes = signal(false);

  // Available theaters for selected movie
  quickAvailableTheaters = computed(() => {
    const sts = this.quickMovieShowtimes();
    const allTheaters = this.homeStore.theaters();
    if (!this.quickMovieId() || sts.length === 0) return allTheaters;
    const theaterIds = new Set(sts.map((s) => s.theaterId));
    return allTheaters.filter((t) => theaterIds.has(t.id));
  });

  // Available dates for selected movie + theater
  quickAvailableDates = computed(() => {
    const sts = this.quickMovieShowtimes();
    const theaterId = this.quickTheaterId();
    if (!theaterId) return [];
    const dateMap = new Map<string, string>();
    for (const st of sts) {
      if (st.theaterId === theaterId && st.showDate) {
        const d = new Date(st.showDate);
        const iso = d.toISOString().split('T')[0];
        if (!dateMap.has(iso)) {
          const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
          const label = `${dayNames[d.getDay()]} - ${d.getDate()}/${d.getMonth() + 1}`;
          dateMap.set(iso, label);
        }
      }
    }
    return Array.from(dateMap.entries()).map(([iso, label]) => ({ iso, label }));
  });

  // Available showtimes for selected movie + theater + date
  quickAvailableShowtimes = computed(() => {
    const sts = this.quickMovieShowtimes();
    const theaterId = this.quickTheaterId();
    const dateStr = this.quickDate();
    if (!theaterId || !dateStr) return [];
    return sts.filter((st) => {
      if (st.theaterId !== theaterId || !st.showDate) return false;
      const iso = new Date(st.showDate).toISOString().split('T')[0];
      return iso === dateStr;
    });
  });

  constructor() {
    effect(() => {
      if (this.homeStore.theaters().length > 0) {
        untracked(() => this.homeStore.initData());
      }
    });
  }

  ngOnInit() {
    this.homeStore.initData();
  }

  reload() {
    this.homeStore.clearError();
    this.homeStore.initData();
  }

  onSelectedMovie(movieId: number) {
    this.homeStore.handleMovieSelection(movieId);
  }

  // Quick ribbon handlers
  onQuickMovieChange(movieId: number | null) {
    this.quickMovieId.set(movieId);
    this.quickTheaterId.set(null);
    this.quickDate.set(null);
    this.quickShowtimeId.set(null);
    this.quickMovieShowtimes.set([]);

    if (movieId) {
      this.quickLoadingShowtimes.set(true);
      this.showtimeService.getAvailableShowtimeByMovie(movieId).subscribe({
        next: (sts) => {
          this.quickMovieShowtimes.set(sts || []);
          this.quickLoadingShowtimes.set(false);
          // Auto-select active theater if available
          const activeTheaterId = this.homeStore.selectedTheaterId();
          const avail = this.quickAvailableTheaters();
          if (activeTheaterId && avail.some((t) => t.id === activeTheaterId)) {
            this.onQuickTheaterChange(activeTheaterId);
          } else if (avail.length > 0) {
            this.onQuickTheaterChange(avail[0].id);
          }
        },
        error: () => {
          this.quickLoadingShowtimes.set(false);
        },
      });
    }
  }

  onQuickTheaterChange(theaterId: number | null) {
    this.quickTheaterId.set(theaterId);
    this.quickDate.set(null);
    this.quickShowtimeId.set(null);

    const dates = this.quickAvailableDates();
    if (dates.length > 0) {
      this.onQuickDateChange(dates[0].iso);
    }
  }

  onQuickDateChange(dateIso: string | null) {
    this.quickDate.set(dateIso);
    this.quickShowtimeId.set(null);

    const sts = this.quickAvailableShowtimes();
    if (sts.length > 0) {
      this.quickShowtimeId.set(sts[0].id);
    }
  }

  onQuickShowtimeChange(stId: number | null) {
    this.quickShowtimeId.set(stId);
  }

  submitQuickBooking() {
    const stId = this.quickShowtimeId();
    if (!stId) return;
    this.homeStore.proceedToReservation(stId);
  }

  formatShowtimeTime(date: Date | string | undefined): string {
    if (!date) return '--:--';
    return new Date(date).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }
}
