import {
  Component,
  computed,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { Store } from '@ngrx/store';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { selectJwt } from '@core/auth/auth.selectors';
import { TranslatePipe } from '@ngx-translate/core';
import { VoucherStore } from '@domain/voucher/data-access/voucher.store';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';

interface SuggestedVoucher {
  code: string;
  desc: string;
}

interface PaymentOption {
  value: string;
  label: string;
  desc?: string;
  icon: string;
  alt: string;
}

@Component({
  selector: 'app-confirm',
  imports: [
    NzIconModule,
    FormsModule,
    NzRadioModule,
    NzButtonModule,
    NzInputModule,
    NzToolTipModule,
    CurrencyPipe,
    TranslatePipe,
  ],
  templateUrl: './confirm.component.html',
  styleUrl: './confirm.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmComponent {
  private store = inject(Store);
  readonly voucherStore = inject(VoucherStore);
  readonly seatStore = inject(SeatStore);
  user = this.store.selectSignal(selectJwt);
  radioValue = 'vnpay';
  voucherCode = '';

  constructor() {
    this.voucherStore.clearCalculation();
  }

  readonly suggestedVouchers: SuggestedVoucher[] = [
    { code: 'WELCOME10', desc: 'voucher.suggest.10k' },
    { code: 'CINEPLEX20', desc: 'voucher.suggest.20k' },
    { code: 'NEWUSER', desc: 'voucher.suggest.15pct' },
  ];

  readonly paymentOptions: PaymentOption[] = [
    {
      value: 'vnpay',
      label: 'VNPAY QR',
      desc: 'confirm.pay.vnpayDesc',
      icon: 'assets/payment/vnpay.svg',
      alt: 'VNPay',
    },
    {
      value: 'momo',
      label: 'confirm.pay.momo',
      icon: 'assets/payment/momo.ico',
      alt: 'Momo',
    },
    {
      value: 'zalopay',
      label: 'confirm.pay.zalopay',
      icon: 'assets/payment/zalopay.png',
      alt: 'ZaloPay',
    },
    {
      value: 'atm',
      label: 'confirm.pay.atm',
      desc: 'confirm.pay.atmDesc',
      icon: 'assets/payment/ic-card-vn.png',
      alt: 'ATM',
    },
    {
      value: 'visa',
      label: 'confirm.pay.visa',
      desc: 'confirm.pay.visaDesc',
      icon: 'assets/payment/ic-card-gb.png',
      alt: 'VISA',
    },
  ];

  readonly appliedVoucher = computed(() => this.voucherStore.calculateResult());
  readonly isValidatingVoucher = computed(() =>
    this.voucherStore.loadingDetails(),
  );
  readonly voucherError = computed(() => this.voucherStore.error());

  selectSuggested(code: string) {
    this.voucherCode = code;
    this.applyVoucher();
  }

  applyVoucher() {
    const code = this.voucherCode.trim();
    if (!code) return;
    this.voucherStore.clearError();
    this.voucherStore.calculateDiscount({
      code,
      total: this.seatStore.totalPrice(),
    });
  }

  removeVoucher() {
    this.voucherCode = '';
    this.voucherStore.clearError();
  }
}
