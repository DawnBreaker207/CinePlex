import {
  Component,
  inject,
  input,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NgClass } from '@angular/common';
import { NzBadgeComponent } from 'ng-zorro-antd/badge';
import {
  NzBreadCrumbComponent,
  NzBreadCrumbItemComponent,
} from 'ng-zorro-antd/breadcrumb';
import {
  NzDropDownDirective,
  NzDropdownMenuComponent,
} from 'ng-zorro-antd/dropdown';
import { NzHeaderComponent } from 'ng-zorro-antd/layout';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { AuthMenuComponent } from '@shared/components/auth-menu/auth-menu.component';
import { Breadcrumb } from '../admin-layout.component';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { InsightService } from '@domain/ai/data-access/insight.service';
import { Insight } from '@domain/ai/models/insight.model';
import { SocketService } from '@core/services/socket/socket.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MovieService } from '@domain/movie/data-access/movie.service';
import { TheaterService } from '@domain/theater/data-access/theater.service';
import {
  Subject,
  switchMap,
  debounceTime,
  catchError,
  of,
  forkJoin,
  map,
} from 'rxjs';

export interface NavSuggestion {
  kind: 'movie' | 'theater' | 'page';
  title: string;
  subtitle?: string;
  icon: string;
  route: string[];
  query?: string;
}

const ADMIN_PAGES: NavSuggestion[] = [
  { kind: 'page', title: 'Phim', icon: 'play-square', route: ['/admin/movie'] },
  { kind: 'page', title: 'Rạp', icon: 'shop', route: ['/admin/theater'] },
  { kind: 'page', title: 'Suất chiếu', icon: 'calendar', route: ['/admin/showtime'] },
  { kind: 'page', title: 'Vé đã đặt', icon: 'ordered-list', route: ['/admin/reservation'] },
  { kind: 'page', title: 'Check-in', icon: 'scan', route: ['/admin/checkin'] },
  { kind: 'page', title: 'Voucher', icon: 'tags', route: ['/admin/voucher'] },
  { kind: 'page', title: 'Người dùng', icon: 'team', route: ['/admin/user'] },
  { kind: 'page', title: 'Bài viết', icon: 'file-text', route: ['/admin/article'] },
  { kind: 'page', title: 'Nhật ký hoạt động', icon: 'audit', route: ['/admin/audit-logs'] },
];

@Component({
  selector: 'app-navbar',
  imports: [
    NzBadgeComponent,
    NzBreadCrumbComponent,
    NzBreadCrumbItemComponent,
    NzDropDownDirective,
    NzDropdownMenuComponent,
    NzHeaderComponent,
    NzIconDirective,
    NzToolTipModule,
    AuthMenuComponent,
    RouterLink,
    TranslatePipe,
    NgClass,
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  breadcrumbs = input<Breadcrumb[]>([]);

  private readonly router = inject(Router);
  private readonly insightsSvc = inject(InsightService);
  private readonly socket = inject(SocketService);
  private readonly movieSvc = inject(MovieService);
  private readonly theaterSvc = inject(TheaterService);
  readonly unreadInsights = this.insightsSvc.unread;
  readonly searchTerm = signal('');
  readonly suggestions = signal<NavSuggestion[]>([]);
  readonly suggestionsOpen = signal(false);
  readonly activeIndex = signal(0);
  private readonly search$ = new Subject<string>();

  constructor() {
    this.insightsSvc.load().subscribe();
    this.socket
      .watchEvent<{ action: string }>('/topic/dashboard/update')
      .pipe(
        switchMap((data) =>
          data?.action === 'REFRESH' ? this.insightsSvc.load() : [],
        ),
        takeUntilDestroyed(),
      )
      .subscribe();

    this.search$
      .pipe(
        debounceTime(300),
        switchMap((q) => {
          const pages = this.matchPages(q);
          const trimmed = q.trim();
          const term = normalize(trimmed);
          if (trimmed.length < 2) {
            return of(pages);
          }
          return forkJoin({
            movies: this.movieSvc
              .getMovieLists({ size: 50 })
              .pipe(catchError(() => of(null))),
            theaters: this.theaterSvc
              .getTheaters({ size: 50 })
              .pipe(catchError(() => of(null))),
          }).pipe(
            map(({ movies, theaters }) => [
              ...pages,
              ...(movies?.content ?? [])
                .filter((m) => normalize(m.title).includes(term))
                .slice(0, 5)
                .map(
                  (m): NavSuggestion => ({
                    kind: 'movie',
                    title: m.title,
                    subtitle: m.releaseDate
                      ? new Date(m.releaseDate).toLocaleDateString('vi-VN')
                      : undefined,
                    icon: 'play-square',
                    route: ['/admin/movie'],
                    query: m.title,
                  }),
                ),
              ...(theaters?.content ?? [])
                .filter((t) => normalize(t.name).includes(term))
                .slice(0, 5)
                .map(
                  (t): NavSuggestion => ({
                    kind: 'theater',
                    title: t.name,
                    subtitle: t.location,
                    icon: 'shop',
                    route: ['/admin/theater'],
                    query: t.name,
                  }),
                ),
            ]),
            catchError(() => of(pages)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((items) => this.suggestions.set(items));
  }

  markRead(insight: Insight) {
    this.insightsSvc.markRead(insight);
  }

  severityIcon(severity: string): string {
    switch (severity) {
      case 'success':
        return 'check-circle';
      case 'warning':
        return 'exclamation-circle';
      default:
        return 'bulb';
    }
  }

  markAllRead() {
    this.insightsSvc.markAllRead();
  }

  onSearchInput(value: string) {
    const q = value.trim();
    this.searchTerm.set(q);
    this.suggestionsOpen.set(q.length > 0);
    this.activeIndex.set(0);
    this.search$.next(q);
  }

  onKeydown(event: KeyboardEvent) {
    const items = this.suggestions();
    if (event.key === 'Escape') {
      this.suggestionsOpen.set(false);
      this.activeIndex.set(0);
      return;
    }
    if (this.suggestionsOpen() && items.length) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        this.activeIndex.set((this.activeIndex() + 1) % items.length);
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        this.activeIndex.set(
          (this.activeIndex() - 1 + items.length) % items.length,
        );
      } else if (event.key === 'Enter') {
        event.preventDefault();
        this.select(items[this.activeIndex()]);
      }
    } else if (event.key === 'Enter' && this.searchTerm()) {
      event.preventDefault();
      this.router.navigate(['/admin/movie'], {
        queryParams: { q: this.searchTerm() },
      });
    }
  }

  select(story: NavSuggestion) {
    this.suggestionsOpen.set(false);
    this.activeIndex.set(0);
    if (story.query) {
      this.router.navigate(story.route, {
        queryParams: { q: story.query },
      });
    } else {
      this.router.navigate(story.route);
    }
  }

  closeSuggestions() {
    setTimeout(() => this.suggestionsOpen.set(false), 150);
  }

  private matchPages(q: string): NavSuggestion[] {
    const term = normalize(q);
    if (!term) return [];
    return ADMIN_PAGES.filter((p) => {
      const t = normalize(p.title);
      return t.includes(term) || term.includes(t);
    });
  }
}

const normalize = (s: string): string =>
  s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');