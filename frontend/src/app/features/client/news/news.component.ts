import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { ArticleService } from '@domain/article/data-access/article.service';
import { Article, ArticleStatus } from '@domain/article/models/article.model';
import { VoucherService } from '@domain/voucher/data-access/voucher.service';
import { VoucherStore } from '@domain/voucher/data-access/voucher.store';
import { Voucher } from '@domain/voucher/model/voucher.model';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { Router } from '@angular/router';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-news',
  imports: [
    DatePipe,
    CurrencyPipe,
    NzEmptyModule,
    NzButtonModule,
    NzSkeletonModule,
    NzTagModule,
    TranslatePipe,
    ErrorStateComponent,
  ],
  templateUrl: './news.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsComponent implements OnInit {
  private articleService = inject(ArticleService);
  private voucherService = inject(VoucherService);
  readonly voucherStore = inject(VoucherStore);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly articles = signal<Article[]>([]);
  readonly vouchers = signal<Voucher[]>([]);
  readonly loading = signal(false);
  readonly articlesError = signal(false);
  readonly expandedId = signal<number | null>(null);

  readonly published = computed(() =>
    this.articles().filter((a) => a.status === ArticleStatus.PUBLISHED),
  );

  ngOnInit() {
    this.loadArticles();
    this.voucherService.getAll({ page: 0, size: 20 }).subscribe({
      next: (page) =>
        this.vouchers.set((page.content ?? []).filter((v) => v.isActive)),
      error: () => this.vouchers.set([]),
    });
  }

  loadArticles() {
    this.loading.set(true);
    this.articlesError.set(false);
    this.articleService
      .getAll({ page: 0, size: 20 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.articles.set(page.content ?? []),
        error: () => {
          this.articles.set([]);
          this.articlesError.set(true);
        },
      });
  }

  toggle(id: number) {
    this.expandedId.update((cur) => (cur === id ? null : id));
  }

  claim(code: string) {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/news' } });
      return;
    }
    // Success toast fires inside the store on real success only.
    this.voucherStore.claimVoucher(code);
  }
}
