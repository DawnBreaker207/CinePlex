import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { MovieItemComponent } from '@shared/components/movie-item/movie-item.component';
import {
  selectAllMovies,
  selectMovieLoading,
} from '@domain/movie/data-access/movie.selectors';
import { TranslatePipe } from '@ngx-translate/core';
import dayjs from 'dayjs';
import { Movie } from '@domain/movie/models/movie.model';

@Component({
  selector: 'app-movies',
  imports: [
    FormsModule,
    RouterLink,
    TranslatePipe,
    NzInputModule,
    NzButtonModule,
    NzIconModule,
    NzTabsModule,
    NzGridModule,
    NzEmptyModule,
    NzSkeletonModule,
    MovieItemComponent,
  ],
  templateUrl: './movies.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MoviesComponent implements OnInit {
  private store = inject(Store);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly loading = this.store.selectSignal(selectMovieLoading);
  readonly allMovies = this.store.selectSignal(selectAllMovies);

  readonly search = signal('');
  readonly tabIndex = signal(0);

  readonly tabTitles = computed(() => {
    const total = this.allMovies().length;
    const now = this.allMovies().filter(
      (m) => !dayjs(m.releaseDate).isAfter(dayjs(), 'day'),
    ).length;
    const upcoming = total - now;
    return {
      all: 'Tất cả (' + total + ')',
      now: 'Đang chiếu (' + now + ')',
      upcoming: 'Sắp chiếu (' + upcoming + ')',
    };
  });

  readonly filtered = computed(() => {
    const movies = this.allMovies();
    const q = this.search().toLowerCase().trim();
    const tab = this.tabIndex();

    return movies.filter((m) => {
      if (tab === 1 && dayjs(m.releaseDate).isAfter(dayjs(), 'day'))
        return false;
      if (tab === 2 && !dayjs(m.releaseDate).isAfter(dayjs(), 'day'))
        return false;
      if (q && !m.title.toLowerCase().includes(q)) return false;
      return true;
    });
  });

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      const tab = params['tab'];
      if (tab === 'now') this.tabIndex.set(1);
      else if (tab === 'upcoming') this.tabIndex.set(2);
      else this.tabIndex.set(0);
    });
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
