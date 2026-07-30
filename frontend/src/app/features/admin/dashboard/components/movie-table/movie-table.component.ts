import { Component, input } from '@angular/core';
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
  selector: 'app-movie-table',
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
  templateUrl: './movie-table.component.html',
})
export class MovieTableComponent {
  data = input<TopTheater[]>([]);
}
