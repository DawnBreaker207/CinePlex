import {
  Component,
  computed,
  input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsCoreOption } from 'echarts/core';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { RevenuePoint, DailyForecast } from '@domain/dashboard/models/dashboard.model';
import { TranslatePipe } from '@ngx-translate/core';
import { CHART_COLORS } from '@shared/utils/colors';
import dayjs from 'dayjs';

@Component({
  selector: 'app-revenue-chart',
  imports: [NgxEchartsDirective, NzIconModule, TranslatePipe],
  templateUrl: './revenue-chart.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueChartComponent {
  revenues = input<RevenuePoint[]>([]);
  forecast = input<DailyForecast[]>([]);
  granularity = input<'day' | 'week' | 'month'>('day');

  private readonly grouped = computed(() => {
    const revenues = this.revenues();
    const granularity = this.granularity();
    if (granularity === 'day') {
      return revenues.map((r) => ({
        label: dayjs(r.date).format('DD/MM'),
        value: r.revenue,
      }));
    }
    const key =
      granularity === 'week'
        ? (d: dayjs.Dayjs) => d.startOf('week')
        : (d: dayjs.Dayjs) => d.startOf('month');
    const map = new Map<string, { label: string; sum: number }>();
    for (const r of revenues) {
      const bucket = key(dayjs(r.date));
      const id = bucket.toISOString();
      const entry = map.get(id) ?? {
        label:
          granularity === 'week'
            ? bucket.format('DD/MM')
            : bucket.format('MM/yyyy'),
        sum: 0,
      };
      entry.sum += r.revenue;
      map.set(id, entry);
    }
    return [...map.values()].map((p) => ({ label: p.label, value: p.sum }));
  });

  options = computed<EChartsCoreOption>(() => {
    const points = this.grouped();
    const forecastPoints = this.forecast().map((f) => ({
      label: dayjs(f.date).format('DD/MM'),
      value: f.projectedRevenue,
    }));
    const date = [
      ...points.map((p) => p.label),
      ...forecastPoints.map((p) => p.label),
    ];
    const revenue = points.map((p) => p.value);
    const primary = CHART_COLORS.primary();
    const gold = '#e0ac52';
    return {
      tooltip: {
        trigger: 'axis',
      },
      legend: {
        show: forecastPoints.length > 0,
        data: ['Revenue', 'Forecast'],
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
        ...(forecastPoints.length > 0
          ? [
              {
                name: 'Forecast',
                type: 'line' as const,
                data: [...revenue.map(() => null), ...forecastPoints.map((p) => p.value)],
                smooth: true,
                showSymbol: false,
                itemStyle: { color: gold },
                lineStyle: { width: 2, type: 'dashed' as const, color: gold },
              },
            ]
          : []),
      ],
    };
  });
}
