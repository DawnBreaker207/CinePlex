import { DOCUMENT } from '@angular/common';
import { DestroyRef, inject, Injectable, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { PaymentService } from '@domain/payment/data-access/payment.service';
import { StorageService } from '@core/services/storage/storage.service';
import { ToastService } from '@core/services/toast/toast.service';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root',
})
export class NetworkService {
  private toast = inject(ToastService);
  private translate = inject(TranslateService);
  private zone = inject(NgZone);
  private window = inject(DOCUMENT).defaultView!;
  private storage = inject(StorageService);
  private paymentService = inject(PaymentService);
  private router = inject(Router);
  private onlineHandler = () => this.zone.run(() => this.onOnline());
  private offlineHandler = () => this.zone.run(() => this.onOffline());

  constructor() {
    this.window.addEventListener('online', this.onlineHandler);
    this.window.addEventListener('offline', this.offlineHandler);
    inject(DestroyRef).onDestroy(() => {
      this.window.removeEventListener('online', this.onlineHandler);
      this.window.removeEventListener('offline', this.offlineHandler);
    });
    // S2: returning to the app after a mid-payment drop (killed tab, dead
    // network on the gateway page). Quiet: only surfaces on a real PAID.
    this.checkPendingPayment(false);
  }

  private onOnline() {
    this.toast.createNotification({
      type: 'success',
      title: this.translate.instant('network.onlineTitle'),
      message: this.translate.instant('network.onlineMsg'),
    });
    this.checkPendingPayment(true);
  }

  // S2: a payment that was in flight when connectivity broke. If the gateway
  // callback already confirmed it server-side, take the user to the result;
  // otherwise surface one "still processing" notice.
  private checkPendingPayment(announce: boolean) {
    const last = this.storage.getItem<{
      provider: string;
      reservationId: string;
    }>('lastPayment');
    if (!last?.reservationId || !last?.provider) return;
    this.paymentService
      .reCheck(last.provider.toLowerCase(), last.reservationId)
      .subscribe({
        next: (state) => {
          if (state === 'PAID') {
            this.storage.removeItem('lastPayment');
            this.toast.createNotification({
              type: 'success',
              title: this.translate.instant('reservation.pendingPaymentTitle'),
              message: this.translate.instant('reservation.pendingPaymentMsg'),
            });
            this.router.navigate(['/paymentResult'], {
              queryParams: {
                status: 'success',
                reservationId: last.reservationId,
              },
            });
          } else if (announce) {
            this.toast.createNotification({
              type: 'info',
              title: this.translate.instant('reservation.recheck'),
              message: this.translate.instant('reservation.stillPending'),
              position: 'top',
            });
          }
        },
        error: () => {},
      });
  }

  private onOffline() {
    this.toast.createNotification({
      type: 'warning',
      title: this.translate.instant('network.offlineTitle'),
      message: this.translate.instant('network.offlineMsg'),
      position: 'topRight',
    });
  }
}
