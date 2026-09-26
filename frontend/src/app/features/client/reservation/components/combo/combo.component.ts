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
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SettingsService } from '@core/services/settings/settings.service';
import { inject } from '@angular/core';

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

  private translate = inject(TranslateService);
  private settings = inject(SettingsService);

  // computed over settings.lang() so names re-translate live on language switch.
  readonly combos = computed<ComboItem[]>(() => {
    void this.settings.lang();
    return [
      {
        name: this.translate.instant('combo.item1.name'),
        desc: this.translate.instant('combo.item1.desc'),
        price: 79000,
      },
      {
        name: this.translate.instant('combo.item2.name'),
        desc: this.translate.instant('combo.item2.desc'),
        price: 139000,
      },
      {
        name: this.translate.instant('combo.item3.name'),
        desc: this.translate.instant('combo.item3.desc'),
        price: 35000,
      },
    ];
  });

  private qtyMap = signal<Record<string, number>>({});
  readonly selections = computed<ComboSelection[]>(() =>
    this.combos()
      .map((c: ComboItem) => ({ name: c.name, price: c.price, qty: this.qtyMap()[c.name] ?? 0 }))
      .filter((s: ComboSelection) => s.qty > 0),
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
