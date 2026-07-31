import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { BarcodeComponent } from '@shared/components/barcode/barcode.component';

export interface TicketMeta {
  label: string;
  value: string;
  accent?: boolean;
}

@Component({
  selector: 'app-ticket-stub',
  imports: [NzIconModule, TranslatePipe, BarcodeComponent],
  templateUrl: './ticket-stub.component.html',
  styleUrls: ['./ticket-stub.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TicketStubComponent {
  readonly title = input.required<string>();
  readonly subtitle = input.required<string>();
  readonly code = input.required<string>();
  readonly meta = input<TicketMeta[]>([]);
  readonly amount = input<string | null>();
}
