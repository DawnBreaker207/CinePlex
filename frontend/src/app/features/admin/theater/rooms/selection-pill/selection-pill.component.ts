import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { TranslatePipe } from '@ngx-translate/core';
import type { SeatTemplateType } from '@domain/theater/models/seat-template.model';

// Floating selection toolbar: count, change type, delete, clear.
// Positioned by the parent next to the last selected seat.
@Component({
  selector: 'app-selection-pill',
  imports: [NzIconModule, NzToolTipModule, NzPopconfirmModule, TranslatePipe],
  templateUrl: './selection-pill.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectionPillComponent {
  readonly count = input.required<number>();
  readonly top = input.required<number | null>();
  readonly left = input.required<number | null>();
  readonly dimmed = input.required<boolean>();
  readonly docked = input.required<boolean>();
  readonly coupleDisabled = input.required<boolean>();
  readonly coupleHint = input.required<string>();
  readonly coupleAllowed = input.required<boolean>();
  readonly pairHint = input.required<string>();
  readonly typeHint = input.required<string>();
  readonly deleteTitle = input.required<string>();
  readonly listOpen = input.required<boolean>();

  readonly pickType = output<SeatTemplateType>();
  readonly requestDelete = output<void>();
  readonly requestClear = output<void>();
  readonly toggleList = output<void>();
}
