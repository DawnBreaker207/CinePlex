import { Component, effect, input } from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TopTheater } from '@domain/dashboard/models/dashboard.model';
import { TranslatePipe } from '@ngx-translate/core';
import { CHART_COLORS } from '@shared/utils/colors';

@Component({
  selector: 'app-theater-chart',
  imports: [NgxEchartsDirective, NzIconModule, TranslatePipe],
  templateUrl: './theater-chart.component.html',
})
export class TheaterChartComponent {
  theaters = input<TopTheater[]>([]);
  options!: EChartsCoreOption;

  constructor() {
    effect(() => {
      const theaters = this.theaters();
      const name = theaters.map(t => t.theaterName);
      const data = theaters.map(t => t.totalRevenue);
      this.loadChartData(name, data);
    });
  }

  loadChartData(name: string[], data: number[]) {
    this.options = {
      tooltip: {
        trigger: 'item',
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
        data: name,
        axisLabel: {
          interval: 0,
          rotate: 0,
          color: CHART_COLORS.textSecondary(),
        },
        axisTick: {
          show: false,
        },
        axisLine: {
          lineStyle: {
            color: CHART_COLORS.border(),
          },
        },
      },
      yAxis: {
        type: 'value',
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
          type: 'bar',
          barWidth: '40%',
          data: data,
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: CHART_COLORS.primary() },
                { offset: 1, color: CHART_COLORS.primary() + '66' },
              ],
            },
            borderRadius: [4, 4, 0, 0],
          },
        },
      ],
    };
  }
}
