import {
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { fromEvent, throttleTime } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { MovieItemComponent } from '@shared/components/movie-item/movie-item.component';
import {
  selectAllMovies,
  selectMovieLoading,
  selectMoviesError,
} from '@domain/movie/data-access/movie.selectors';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';
import { errorMessage } from '@shared/utils/error.helper';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs';
import { Movie } from '@domain/movie/models/movie.model';
import { MovieActions } from '@domain/movie/data-access/movie.actions';

@Component({
  selector: 'app-movies',
  imports: [
    FormsModule,
    TranslatePipe,
    NzInputModule,
    NzButtonModule,
    NzIconModule,
    NzTabsModule,
    NzGridModule,
    NzEmptyModule,
    NzSkeletonModule,
    NzSelectModule,
    MovieItemComponent,
    ErrorStateComponent,
  ],
  templateUrl: './movies.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MoviesComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  readonly loading = this.store.selectSignal(selectMovieLoading);
  readonly allMovies = this.store.selectSignal(selectAllMovies);
  readonly loadError = computed(() => {
    const err = this.store.selectSignal(selectMoviesError)();
    return err ? errorMessage(err) : null;
  });

  readonly search = signal('');
  readonly tabIndex = signal(0);
  readonly sort = signal<'newest' | 'oldest'>('newest');
  readonly visibleCount = signal(18);
  private destroyRef = inject(DestroyRef);

  readonly tabTitles = computed(() => {
    const total = this.allMovies().length;
    const now = this.allMovies().filter(
      (m) => !dayjs(m.releaseDate).isAfter(dayjs(), 'day'),
    ).length;
    const upcoming = total - now;
    const t = this.translate.instant.bind(this.translate);
    return {
      all: t('movies.all') + ' (' + total + ')',
      now: t('movies.now') + ' (' + now + ')',
      upcoming: t('movies.upcoming') + ' (' + upcoming + ')',
    };
  });

  readonly filtered = computed(() => {
    const movies = this.allMovies();
    const q = this.search().toLowerCase().trim();
    const tab = this.tabIndex();
    const sort = this.sort();

    const list = movies.filter((m) => {
      if (tab === 1 && dayjs(m.releaseDate).isAfter(dayjs(), 'day'))
        return false;
      if (tab === 2 && !dayjs(m.releaseDate).isAfter(dayjs(), 'day'))
        return false;
      if (q && !m.title.toLowerCase().includes(q)) return false;
      return true;
    });
    return [...list].sort((a, b) => {
      const diff =
        dayjs(a.releaseDate).valueOf() - dayjs(b.releaseDate).valueOf();
      return sort === 'newest' ? -diff : diff;
    });
  });

  readonly visible = computed(() =>
    this.filtered().slice(0, this.visibleCount()),
  );

  ngOnInit() {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const tab = params['tab'];
        if (tab === 'now') this.onTabChange(1);
        else if (tab === 'upcoming') this.onTabChange(2);
        else this.onTabChange(0);
      });
    fromEvent(window, 'scroll')
      .pipe(throttleTime(200), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.maybeGrow());
  }

  // Reset the infinite-scroll window whenever a filter changes.
  onSearchChange(value: string) {
    this.search.set(value);
    this.visibleCount.set(18);
  }

  onTabChange(index: number) {
    this.tabIndex.set(index);
    this.visibleCount.set(18);
  }

  onSortChange(value: 'newest' | 'oldest') {
    this.sort.set(value);
    this.visibleCount.set(18);
  }

  maybeGrow() {
    const nearBottom =
      window.innerHeight + window.scrollY >=
      document.body.scrollHeight - 600;
    if (nearBottom && this.visibleCount() < this.filtered().length) {
      this.visibleCount.update((n) => n + 12);
    }
  }

  clearFilter() {
    this.search.set('');
    this.tabIndex.set(0);
    this.sort.set('newest');
    this.visibleCount.set(18);
  }

  reload() {
    this.store.dispatch(MovieActions.loadMovies({ page: 0, size: 18 }));
  }

  isNowShowing(movie: Movie) {
    return !dayjs(movie.releaseDate).isAfter(dayjs(), 'day');
  }

  trackById(_: number, m: Movie) {
    return m.id;
  }

  onSelectMovie(movieId: number) {
    this.router.navigate(['/movie/detail', movieId]);
  }
}
