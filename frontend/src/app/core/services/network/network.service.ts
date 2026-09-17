import { DOCUMENT } from '@angular/common';
import { DestroyRef, inject, Injectable, NgZone } from '@angular/core';
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
  private onlineHandler = () => this.zone.run(() => this.onOnline());
  private offlineHandler = () => this.zone.run(() => this.onOffline());

  constructor() {
    this.window.addEventListener('online', this.onlineHandler);
    this.window.addEventListener('offline', this.offlineHandler);
    inject(DestroyRef).onDestroy(() => {
      this.window.removeEventListener('online', this.onlineHandler);
      this.window.removeEventListener('offline', this.offlineHandler);
    });
  }

  private onOnline() {
    this.toast.createNotification({
      type: 'success',
      title: this.translate.instant('network.onlineTitle'),
      message: this.translate.instant('network.onlineMsg'),
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
