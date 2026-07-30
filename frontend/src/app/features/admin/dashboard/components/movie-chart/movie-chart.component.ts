import { Component, effect, input } from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TopMovie } from '@domain/dashboard/models/dashboard.model';
import { TranslatePipe } from '@ngx-translate/core';
import { CHART_COLORS } from '@shared/utils/colors';

@Component({
  selector: 'app-movie-chart',
  imports: [NgxEchartsDirective, NzIconModule, TranslatePipe],
  templateUrl: './movie-chart.component.html',
})
export class MovieChartComponent {
  movies = input<TopMovie[]>([]);
  options!: EChartsCoreOption;

  constructor() {
    effect(() => {
      const movies = this.movies();
      const name = movies.map(m => m.movieName);
      const data = movies.map(m => m.revenue);
      this.loadChartData(name, data);
    });
  }

  loadChartData(name: string[], data: number[]) {
    this.options = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
      },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'value',
        boundaryGap: [0, 0.01],
        splitLine: { lineStyle: { type: 'dashes', color: CHART_COLORS.border() } },
      },
      yAxis: {
        type: 'category',
        data: name,
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          name: 'Revenue',
          type: 'bar',
          data: data,
          itemStyle: {
            color: CHART_COLORS.primary(),
            borderRadius: [0, 4, 4, 0],
          },
          barWidth: 20,
        },
      ],
    };
  }
}
