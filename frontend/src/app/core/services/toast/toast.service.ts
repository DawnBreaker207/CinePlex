import { inject, Injectable, Injector } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { NzNotificationService } from 'ng-zorro-antd/notification';

type notificationInfo = {
  type: 'success' | 'info' | 'warning' | 'error';
  title: string;
  message: string;
  position:
    | 'top'
    | 'bottom'
    | 'topLeft'
    | 'topRight'
    | 'bottomLeft'
    | 'bottomRight';
};

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private notification = inject(NzNotificationService);
  private injector = inject(Injector);

  createNotification(info: Partial<notificationInfo> = {}) {
    // NOTE: TranslateService is resolved lazily (not in the constructor).
    // Eager injection creates a DI cycle that breaks ALL http requests:
    // HttpClient → ErrorInterceptor → GlobalErrorService → ToastService
    // → TranslateService → TranslateLoader → HttpClient (NG0200).
    // The injector.get() calls below only run when a default is actually
    // needed, keeping TranslateService out of the constructor graph.
    const title: string =
      info.title ?? this.injector.get(TranslateService).instant('error.title');
    const message: string =
      info.message ?? this.injector.get(TranslateService).instant('error.unknown');
    this.notification.create(info.type ?? 'info', title, message, {
      nzPlacement: info.position ?? 'topRight',
      nzDuration: 2500,
    });
  }
}
