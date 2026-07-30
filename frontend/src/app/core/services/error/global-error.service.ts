import { inject, Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@core/services/toast/toast.service';

@Injectable({
  providedIn: 'root',
})
export class GlobalErrorService {
  private toast = inject(ToastService);

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
    const status = error.status;
    let type = 'error';
    let title = `Lỗi ${status}`;
    const serverMessage = error.error?.message;
    let message = serverMessage || error.message || 'Lỗi không xác định';

    switch (status) {
      case 400:
        title = `${status}: Yêu cầu không hợp lệ`;
        if (!serverMessage) message = 'Dữ liệu gửi lên không đúng. Vui lòng kiểm tra lại.';
        break;
      case 401:
        title = `${status}: Chưa đăng nhập`;
        if (!serverMessage) message = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
        break;
      case 403:
        type = 'warning';
        title = `${status}: Không có quyền truy cập`;
        if (!serverMessage) message = 'Bạn không có quyền thực hiện thao tác này.';
        break;
      case 404:
        title = `${status}: Không tìm thấy`;
        if (!serverMessage) message = 'Dữ liệu yêu cầu không tồn tại.';
        break;
      case 429:
        type = 'warning';
        title = `${status}: Quá nhiều yêu cầu`;
        if (!serverMessage) message = 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.';
        break;
      case 500:
        title = `${status}: Lỗi máy chủ`;
        if (!serverMessage) message = 'Máy chủ gặp sự cố. Vui lòng thử lại sau.';
        break;
      case 503:
        type = 'warning';
        title = `${status}: Dịch vụ tạm ngưng`;
        if (!serverMessage) message = 'Dịch vụ đang bảo trì. Vui lòng thử lại sau.';
        break;
      default:
        title = `Lỗi ${status}`;
        break;
    }
    return { type, title, message };
  }
}
