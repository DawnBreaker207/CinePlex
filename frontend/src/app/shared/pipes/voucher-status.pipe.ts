import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'voucherStatus',
})
export class VoucherStatusPipe implements PipeTransform {
  private readonly labels: Record<string, string> = {
    ACTIVE: 'voucher.status.active',
    PAUSED: 'voucher.status.paused',
    EXPIRED: 'voucher.status.expired',
    SCHEDULED: 'voucher.status.scheduled',
    EXHAUSTED: 'voucher.status.exhausted',
    PENDING_APPROVAL: 'voucher.status.pending',
  };

  private readonly colors: Record<string, string> = {
    ACTIVE: 'success',
    PAUSED: 'warning',
    SCHEDULED: 'warning',
    PENDING_APPROVAL: 'warning',
    EXPIRED: 'error',
    EXHAUSTED: 'error',
  };

  transform(status: string): { label: string; color: string } {
    return {
      label: this.labels[status] ?? 'common.unknown',
      color: this.colors[status] ?? 'default',
    };
  }
}
