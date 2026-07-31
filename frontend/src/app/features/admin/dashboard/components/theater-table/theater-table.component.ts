import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { NzTableModule } from 'ng-zorro-antd/table';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { TopMovie } from '@domain/dashboard/models/dashboard.model';

@Component({
  selector: 'app-theater-table',
  imports: [NzTableModule, TranslatePipe, CurrencyFormatPipe],
  templateUrl: './theater-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TheaterTableComponent {
  data = input<TopMovie[]>([]);
}
