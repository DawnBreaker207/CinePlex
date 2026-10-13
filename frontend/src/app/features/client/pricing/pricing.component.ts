import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { NzIconModule } from 'ng-zorro-antd/icon';

interface PriceRow {
  type: string;
  day2d: number;
  day3d: number;
  end2d: number;
  end3d: number;
}

@Component({
  selector: 'app-pricing',
  imports: [CurrencyPipe, TranslatePipe, NzIconModule],
  templateUrl: './pricing.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PricingComponent {
  readonly rows: PriceRow[] = [
    { type: 'pricing.standard', day2d: 75000, day3d: 110000, end2d: 95000, end3d: 135000 },
    { type: 'pricing.vip', day2d: 90000, day3d: 130000, end2d: 115000, end3d: 155000 },
    { type: 'pricing.couple', day2d: 160000, day3d: 230000, end2d: 200000, end3d: 270000 },
  ];
}
