import { inject, Pipe, PipeTransform } from '@angular/core';
import { CurrencyPipe } from '@angular/common';

@Pipe({
  name: 'currencyFormat',
})
export class CurrencyFormatPipe implements PipeTransform {
  private currency = inject(CurrencyPipe);

  transform(value: number | null | undefined) {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '0 ₫';
    }
    return this.currency.transform(value, 'VND', 'symbol', '1.0-0');
  }
}
