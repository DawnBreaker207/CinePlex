import {
  Component,
  ChangeDetectionStrategy,
  inject,
  input,
  output,
} from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import type { SeatTemplate } from '@domain/theater/models/seat-template.model';
import {
  FixedCell,
  seatTypeClass,
} from '@shared/utils/seat-layout';

export interface SeatRow {
  row: string;
  seats: SeatTemplate[];
  cells: FixedCell<SeatTemplate>[];
}

// Presentational seat grid: fixed 15-column rows, side aisles, ghost add-row.
// All state lives in the parent; this component only renders and emits.
@Component({
  selector: 'app-seat-grid',
  imports: [NzButtonModule, NzIconModule, NzToolTipModule, TranslatePipe],
  templateUrl: './seat-grid.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeatGridComponent {
  private translate = inject(TranslateService);

  readonly groups = input.required<SeatRow[]>();
  readonly editing = input.required<boolean>();
  readonly coarse = input.required<boolean>();
  readonly selectedIds = input.required<Set<number>>();
  readonly pendingId = input.required<number | null>();
  readonly holdId = input.required<number | null>();
  readonly holdPct = input.required<number>();
  readonly ghosts = input.required<Set<string>>();
  readonly canAddRow = input.required<boolean>();
  readonly pendingLabel = input.required<string | null>();

  readonly seatTap = output<SeatTemplate>();
  readonly holdStart = output<{ seat: SeatTemplate; event: PointerEvent }>();
  readonly holdMove = output<PointerEvent>();
  readonly holdEnd = output<void>();
  readonly emptyAdd = output<{ row: string; n: number }>();
  readonly cellDelete = output<SeatTemplate[]>();
  readonly rowPick = output<string>();
  readonly rowClone = output<string>();
  readonly rowDelete = output<string>();
  readonly rowAdd = output<void>();
  readonly scrollEnd = output<void>();
  readonly cancelPending = output<void>();

  readonly seatTypeClass = seatTypeClass;

  isPending(id: number): boolean {
    return this.pendingId() === id;
  }

  ghostNumber(rowLabel: string, seatNumber: number): string | null {
    return this.ghosts().has(`${rowLabel}-${seatNumber}`)
      ? `${seatNumber}`
      : null;
  }

  seatTooltip(seat: SeatTemplate): string {
    const label = this.translate.instant(`admin.room.type.${seat.seatType}`);
    const name = `${seat.rowLabel}${seat.seatNumber} · ${label}`;
    if (!this.editing()) return name;
    if (seat.seatType === 'COUPLE') {
      return `${name} — ${this.translate.instant('admin.room.splitHint')}`;
    }
    return `${name} — ${this.translate.instant('admin.room.clickToSelect')}`;
  }

  coupleTooltip(rowLabel: string, label: string): string {
    const name = `${rowLabel}${label} · ${this.translate.instant('admin.room.type.COUPLE')}`;
    if (!this.editing()) return name;
    return `${name} — ${this.translate.instant('admin.room.splitHint')}`;
  }
}
