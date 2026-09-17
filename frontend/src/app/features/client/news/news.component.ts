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
import { ToastService } from '@core/services/toast/toast.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { Router } from '@angular/router';
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
  ],
  templateUrl: './news.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsComponent implements OnInit {
  private articleService = inject(ArticleService);
  private voucherService = inject(VoucherService);
  private voucherStore = inject(VoucherStore);
  private toast = inject(ToastService);
  private translate = inject(TranslateService);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly articles = signal<Article[]>([]);
  readonly vouchers = signal<Voucher[]>([]);
  readonly loading = signal(false);
  readonly expandedId = signal<number | null>(null);

  readonly published = computed(() =>
    this.articles().filter((a) => a.status === ArticleStatus.PUBLISHED),
  );

  ngOnInit() {
    this.loading.set(true);
    this.articleService
      .getAll({ page: 0, size: 20 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.articles.set(page.content ?? []),
        error: () => this.articles.set([]),
      });
    this.voucherService.getAll({ page: 0, size: 20 }).subscribe({
      next: (page) =>
        this.vouchers.set((page.content ?? []).filter((v) => v.isActive)),
      error: () => this.vouchers.set([]),
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
    this.voucherStore.claimVoucher(code);
    this.toast.createNotification({
      type: 'success',
      title: code,
      message: this.translate.instant('voucher.claimed'),
      position: 'top',
    });
  }
}
