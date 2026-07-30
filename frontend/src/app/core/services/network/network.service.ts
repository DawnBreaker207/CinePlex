import { DOCUMENT } from '@angular/common';
import { inject, Injectable, NgZone, OnDestroy } from '@angular/core';
import { ToastService } from '@core/services/toast/toast.service';

@Injectable({
  providedIn: 'root',
})
export class NetworkService implements OnDestroy {
  private toast = inject(ToastService);
  private zone = inject(NgZone);
  private window = inject(DOCUMENT).defaultView!;
  private onlineHandler = () => this.zone.run(() => this.onOnline());
  private offlineHandler = () => this.zone.run(() => this.onOffline());

  constructor() {
    this.window.addEventListener('online', this.onlineHandler);
    this.window.addEventListener('offline', this.offlineHandler);
  }

  ngOnDestroy() {
    this.window.removeEventListener('online', this.onlineHandler);
    this.window.removeEventListener('offline', this.offlineHandler);
  }

  private onOnline() {
    this.toast.createNotification({
      type: 'success',
      title: 'Đã kết nối lại',
      message: 'Kết nối mạng đã được khôi phục.',
    });
  }

  private onOffline() {
    this.toast.createNotification({
      type: 'warning',
      title: 'Mất kết nối mạng',
      message: 'Vui lòng kiểm tra lại kết nối Internet.',
      position: 'topRight',
    });
  }
}
