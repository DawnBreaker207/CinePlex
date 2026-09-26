import {
  Component,
  effect,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { VoucherStore } from '@domain/voucher/data-access/voucher.store';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';

@Component({
  selector: 'app-user-vouchers',
  imports: [
    FormsModule,
    NzButtonModule,
    NzInputModule,
    NzEmptyModule,
    NzTagModule,
    NzIconModule,
    CurrencyPipe,
    DatePipe,
    TranslatePipe,
    ErrorStateComponent,
  ],
  templateUrl: './user-vouchers.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserVouchersComponent implements OnInit {
  readonly voucherStore = inject(VoucherStore);
  readonly code = signal('');
  private seenCount = 0;

  constructor() {
    // Clear the input only when a claim actually lands (not on error —
    // the user would have to retype the code).
    effect(() => {
      const n = this.voucherStore.userVouchers().length;
      if (n > this.seenCount && this.code() !== '') this.code.set('');
      this.seenCount = n;
    });
  }

  ngOnInit() {
    this.voucherStore.loadUserVouchers();
  }

  claim() {
    const code = this.code().trim().toUpperCase();
    if (!code || this.voucherStore.saving()) return;
    this.voucherStore.claimVoucher(code);
  }

  statusColor(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'success';
      case 'USED':
        return 'default';
      default:
        return 'error';
    }
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'voucher.status.active';
      case 'USED':
        return 'voucher.used';
      default:
        return 'voucher.status.expired';
    }
  }
}
