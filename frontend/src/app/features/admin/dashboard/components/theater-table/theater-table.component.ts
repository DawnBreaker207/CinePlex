import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import {
  NzTableCellDirective,
  NzTableComponent,
  NzTbodyComponent,
  NzTheadComponent,
  NzThMeasureDirective,
  NzTrDirective,
} from 'ng-zorro-antd/table';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { TopTheater } from '@domain/dashboard/models/dashboard.model';

@Component({
  selector: 'app-theater-table',
  imports: [
    NzTableCellDirective,
    NzTableComponent,
    NzTbodyComponent,
    NzThMeasureDirective,
    NzTheadComponent,
    NzTrDirective,
    TranslatePipe,
    CurrencyFormatPipe,
  ],
  templateUrl: './theater-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TheaterTableComponent {
  data = input<TopTheater[]>([]);
}
