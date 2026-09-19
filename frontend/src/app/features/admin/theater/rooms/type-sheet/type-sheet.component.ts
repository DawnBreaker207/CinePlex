import {
  Component,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzDrawerRef } from 'ng-zorro-antd/drawer';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';
import type { SeatTemplateType } from '@domain/theater/models/seat-template.model';

// Bottom sheet for picking a seat type on touch devices, where the small
// inline dropdown is hard to use. Closes with the picked type (or nothing).
@Component({
  selector: 'app-type-sheet',
  imports: [NzIconModule, TranslatePipe],
  templateUrl: './type-sheet.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TypeSheetComponent {
  private drawerRef = inject(NzDrawerRef);

  // Plain field (not a signal input): NzDrawer applies nzContentParams
  // via Object.assign, which cannot write signal inputs. Assigned before
  // first change detection, so the template reads the right value.
  coupleAllowed = false;

  readonly types: { type: SeatTemplateType; swatch: string }[] = [
    { type: 'NORMAL', swatch: 'bg-slate-300' },
    { type: 'VIP', swatch: 'bg-[var(--color-gold)]' },
  ];

  readonly coupleEntry = { type: 'COUPLE' as const, swatch: 'bg-purple-500' };

  pick(type: SeatTemplateType) {
    this.drawerRef.close(type);
  }
}
