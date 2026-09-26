import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'tags',
})
export class StatusTagsPipe implements PipeTransform {
  private statusColors: Record<string, string> = {
    CONFIRMED: 'green',
    CANCELED: 'red',
    PENDING: 'orange',
    FAILED: 'red',
    EXPIRED: 'default',
    REFUNDED: 'gray',
    REFUND_PENDING: 'orange',

    MOMO: 'purple',
    VNPAY: 'blue',
    ZALOPAY: 'cyan',
    CASH: 'green',

    PAID: 'green',
  };

  transform(status: string): { status: string; color: string } {
    const color = this.statusColors[status] || 'default';
    return { color, status };
  }
}
