import {
  Component,
  computed,
  input,
  output,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsCoreOption } from 'echarts/core';
import { NzIconModule } from 'ng-zorro-antd/icon';
import {
  NzTableCellDirective,
  NzTableComponent,
  NzTbodyComponent,
  NzTheadComponent,
  NzThMeasureDirective,
  NzTrDirective,
} from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { TranslatePipe } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { CHART_COLORS } from '@shared/utils/colors';
import { Operation, OperationAction } from '@domain/dashboard/models/dashboard.model';

type Filter = 'ALL' | Operation['type'];

@Component({
  selector: 'app-operations',
  imports: [
    NgxEchartsDirective,
    NzIconModule,
    NzTableComponent,
    NzTbodyComponent,
    NzTheadComponent,
    NzThMeasureDirective,
    NzTrDirective,
    NzTableCellDirective,
    NzButtonModule,
    TranslatePipe,
    CurrencyFormatPipe,
  ],
  templateUrl: './operations.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OperationsComponent {
  operations = input<Operation[]>([]);
  occupancy = input(0);
  readonly resolve = output<OperationAction>();

  readonly filter = signal<Filter>('ALL');

  readonly counts = computed(() => ({
    FAILED_PAYMENT: this.operations().filter((o) => o.type === 'FAILED_PAYMENT')
      .length,
    REFUND_REQUEST: this.operations().filter((o) => o.type === 'REFUND_REQUEST')
      .length,
  }));

  readonly filtered = computed(() =>
    this.filter() === 'ALL'
      ? this.operations()
      : this.operations().filter((o) => o.type === this.filter()),
  );

  readonly gaugeOptions = computed<EChartsCoreOption>(() => ({
    series: [
      {
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        progress: {
          show: true,
          width: 18,
          itemStyle: { color: CHART_COLORS.primary() },
        },
        axisLine: {
          lineStyle: { width: 18, color: [[1, CHART_COLORS.border()]] },
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 24,
          fontWeight: 'bold',
          formatter: '{value}%',
          color: CHART_COLORS.textSecondary(),
        },
        data: [{ value: this.occupancy() }],
      },
    ],
  }));

  setFilter(filter: Filter) {
    this.filter.set(filter);
  }

  act(op: Operation, action: OperationAction['action']) {
    this.resolve.emit({ id: op.id, type: op.type, action, method: op.method });
  }
}
