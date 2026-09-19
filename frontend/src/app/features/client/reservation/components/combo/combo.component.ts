import {
  Component,
  ChangeDetectionStrategy,
  computed,
  output,
  signal,
} from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { CurrencyPipe, NgClass } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

export interface ComboItem {
  name: string;
  desc: string;
  price: number;
}

export interface ComboSelection {
  name: string;
  price: number;
  qty: number;
}

@Component({
  selector: 'app-combo',
  imports: [NzButtonModule, NzIconModule, CurrencyPipe, NgClass, TranslatePipe],
  templateUrl: './combo.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ComboComponent {
  readonly next = output<void>();
  readonly changed = output<ComboSelection[]>();

  readonly combos: ComboItem[] = [
    { name: 'Combo 1', desc: '1 bắp + 1 nước', price: 79000 },
    { name: 'Combo 2', desc: '2 bắp + 2 nước', price: 139000 },
    { name: 'Combo 3', desc: '1 ly nước lớn', price: 35000 },
  ];

  private qtyMap = signal<Record<string, number>>({});
  readonly selections = computed<ComboSelection[]>(() =>
    this.combos
      .map((c) => ({ name: c.name, price: c.price, qty: this.qtyMap()[c.name] ?? 0 }))
      .filter((s) => s.qty > 0),
  );
  readonly comboTotal = computed(() =>
    this.selections().reduce((sum, s) => sum + s.price * s.qty, 0),
  );

  qtyOf(name: string): number {
    return this.qtyMap()[name] ?? 0;
  }

  changeQty(name: string, delta: number) {
    this.qtyMap.update((map) => {
      const next = Math.max(0, Math.min(10, (map[name] ?? 0) + delta));
      return { ...map, [name]: next };
    });
    this.changed.emit(this.selections());
  }

  goNext() {
    this.next.emit();
  }
}
