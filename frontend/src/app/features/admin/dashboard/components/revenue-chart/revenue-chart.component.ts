import {
  Component,
  computed,
  input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsCoreOption } from 'echarts/core';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { RevenuePoint } from '@domain/dashboard/models/dashboard.model';
import { TranslatePipe } from '@ngx-translate/core';
import { CHART_COLORS } from '@shared/utils/colors';

@Component({
  selector: 'app-revenue-chart',
  imports: [NgxEchartsDirective, NzIconModule, TranslatePipe],
  templateUrl: './revenue-chart.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueChartComponent {
  revenues = input<RevenuePoint[]>([]);
  options = computed<EChartsCoreOption>(() => {
    const revenues = this.revenues();
    const date = revenues.map((r) => r.date.toString());
    const revenue = revenues.map((r) => r.revenue);
    const primary = CHART_COLORS.primary();
    return {
      tooltip: {
        trigger: 'axis',
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
        top: '10%',
      },
      xAxis: {
        type: 'category',
        data: date,
        axisLine: {
          lineStyle: {
            color: CHART_COLORS.border(),
          },
        },
        axisLabel: {
          color: CHART_COLORS.textSecondary(),
        },
      },
      yAxis: {
        type: 'value',
        axisLine: {
          show: false,
        },
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: CHART_COLORS.border(),
          },
        },
      },
      series: [
        {
          name: 'Revenue',
          type: 'line',
          data: revenue,
          smooth: true,
          showSymbol: false,
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                {
                  offset: 0,
                  color: primary + '33',
                },
                {
                  offset: 1,
                  color: primary + '00',
                },
              ],
            },
          },
          itemStyle: {
            color: primary,
          },
          lineStyle: {
            width: 3,
          },
        },
      ],
    };
  });
}
