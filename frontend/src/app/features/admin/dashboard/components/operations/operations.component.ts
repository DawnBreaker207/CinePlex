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
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { CHART_COLORS } from '@shared/utils/colors';
import { Operation } from '@features/admin/dashboard/mocks/dashboard.mock';
import { inject } from '@angular/core';

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
  readonly resolve = output<string>();

  private translate = inject(TranslateService);

  readonly filter = signal<Filter>('ALL');

  readonly counts = computed(() => ({
    FAILED_PAYMENT: this.operations().filter((o) => o.type === 'FAILED_PAYMENT')
      .length,
    REFUND_REQUEST: this.operations().filter((o) => o.type === 'REFUND_REQUEST')
      .length,
    LOW_OCCUPANCY: this.operations().filter((o) => o.type === 'LOW_OCCUPANCY')
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

  readonly funnelOptions = computed<EChartsCoreOption>(() => ({
    tooltip: {
      trigger: 'item',
      formatter: (p: unknown) => {
        const d = p as { name?: string; value?: number };
        return `${this.translate.instant(d.name ?? '')}: ${d.value ?? 0}%`;
      },
    },
    series: [
      {
        type: 'funnel',
        left: '10%',
        width: '80%',
        sort: 'descending',
        gap: 2,
        label: {
          show: true,
          position: 'inside',
          formatter: (p: unknown) => {
            const d = p as { name?: string; value?: number };
            return `${this.translate.instant(d.name ?? '')}: ${d.value ?? 0}%`;
          },
        },
        color: [
          CHART_COLORS.primary(),
          CHART_COLORS.warning(),
          CHART_COLORS.info(),
          CHART_COLORS.muted(),
        ],
        data: [
          { name: 'admin.dashboard.operations.stageInit', value: 100 },
          { name: 'admin.dashboard.operations.stageConfirm', value: 82 },
          { name: 'admin.dashboard.operations.stagePaid', value: 64 },
          { name: 'admin.dashboard.operations.stageRefund', value: 3 },
        ],
      },
    ],
  }));

  setFilter(filter: Filter) {
    this.filter.set(filter);
  }
}
