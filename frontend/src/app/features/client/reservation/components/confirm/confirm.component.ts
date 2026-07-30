import { Component, computed, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
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
    CurrencyPipe,
    TranslatePipe,
  ],
  templateUrl: './confirm.component.html',
  styleUrl: './confirm.component.css',
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
    { code: 'WELCOME10', desc: 'Giảm 10.000₫' },
    { code: 'CINEPLEX20', desc: 'Giảm 20.000₫' },
    { code: 'NEWUSER', desc: 'Giảm 15%' },
  ];

  readonly paymentOptions: PaymentOption[] = [
    { value: 'vnpay', label: 'VNPAY QR', desc: 'Quét mã QR từ ứng dụng ngân hàng', icon: 'assets/payment/vnpay.svg', alt: 'VNPay' },
    { value: 'momo', label: 'Ví Momo', icon: 'assets/payment/momo.ico', alt: 'Momo' },
    { value: 'zalopay', label: 'Ví ZaloPay', icon: 'assets/payment/zalopay.png', alt: 'ZaloPay' },
    { value: 'atm', label: 'Thẻ ATM nội địa', desc: 'Cần đăng ký Internet Banking', icon: 'assets/payment/ic-card-vn.png', alt: 'ATM' },
    { value: 'visa', label: 'Thẻ Visa / Master / JCB', desc: 'Thanh toán quốc tế', icon: 'assets/payment/ic-card-gb.png', alt: 'VISA' },
  ];

  readonly appliedVoucher = computed(() => this.voucherStore.calculateResult());
  readonly isValidatingVoucher = computed(() => this.voucherStore.loadingDetails());
  readonly voucherError = computed(() => this.voucherStore.error());

  selectSuggested(code: string) {
    this.voucherCode = code;
    this.applyVoucher();
  }

  applyVoucher() {
    const code = this.voucherCode.trim();
    if (!code) return;
    this.voucherStore.clearError();
    this.voucherStore.calculateDiscount({ code, total: this.seatStore.totalPrice() });
  }

  removeVoucher() {
    this.voucherCode = '';
    this.voucherStore.clearError();
  }
}
