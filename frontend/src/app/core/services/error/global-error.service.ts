import { inject, Injectable, Injector } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@core/services/toast/toast.service';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root',
})
export class GlobalErrorService {
  private toast = inject(ToastService);
  private injector = inject(Injector);

  readonly RETRY_CONFIG = {
    maxRetries: 3,
    retryableStatus: [429, 503],
    baseDelayMs: 1000,
  };

  handleError(err: HttpErrorResponse, skipNotification: boolean = false) {
    if (skipNotification) return;
    const { title, message, type } = this.parseError(err);
    this.toast.createNotification({
      type: type as 'error' | 'warning',
      title,
      message,
    });
  }

  shouldRetry(status: number) {
    return this.RETRY_CONFIG.retryableStatus.includes(status);
  }

  getRetryDelay(retryCount: number) {
    return Math.pow(2, retryCount) * this.RETRY_CONFIG.baseDelayMs;
  }

  private parseError(error: HttpErrorResponse) {
    const translate = this.injector.get(TranslateService);
    const t = (key: string) => translate.instant(key);
    const status = error.status;
    let type = 'error';
    let title = `${t('error.title')} ${status}`;
    const serverMessage = error.error?.message;
    let message = serverMessage || error.message || t('error.unknown');

    switch (status) {
      case 400:
        title = `${status}: ${t('error.badRequest')}`;
        if (!serverMessage) message = t('error.badRequestMsg');
        break;
      case 401:
        title = `${status}: ${t('error.unauthorized')}`;
        if (!serverMessage) message = t('error.unauthorizedMsg');
        break;
      case 403:
        type = 'warning';
        title = `${status}: ${t('error.forbidden')}`;
        if (!serverMessage) message = t('error.forbiddenMsg');
        break;
      case 404:
        title = `${status}: ${t('error.notFound')}`;
        if (!serverMessage) message = t('error.notFoundMsg');
        break;
      case 429:
        type = 'warning';
        title = `${status}: ${t('error.tooManyRequests')}`;
        if (!serverMessage) message = t('error.tooManyRequestsMsg');
        break;
      case 500:
        title = `${status}: ${t('error.serverError')}`;
        if (!serverMessage) message = t('error.serverErrorMsg');
        break;
      case 503:
        type = 'warning';
        title = `${status}: ${t('error.serviceUnavailable')}`;
        if (!serverMessage) message = t('error.serviceUnavailableMsg');
        break;
      default:
        title = `${t('error.title')} ${status}`;
        break;
    }
    return { type, title, message };
  }
}
