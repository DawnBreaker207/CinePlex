import { Component, ChangeDetectionStrategy, output } from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { CurrencyPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

interface ComboItem {
  name: string;
  desc: string;
  price: number;
}

@Component({
  selector: 'app-combo',
  imports: [NzButtonModule, NzIconModule, CurrencyPipe, TranslatePipe],
  templateUrl: './combo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ComboComponent {
  readonly next = output<void>();

  readonly combos: ComboItem[] = [
    { name: 'Combo 1', desc: '1 bắp + 1 nước', price: 79000 },
    { name: 'Combo 2', desc: '2 bắp + 2 nước', price: 139000 },
    { name: 'Combo 3', desc: '1 ly nước lớn', price: 35000 },
  ];

  goNext() {
    this.next.emit();
  }
}
