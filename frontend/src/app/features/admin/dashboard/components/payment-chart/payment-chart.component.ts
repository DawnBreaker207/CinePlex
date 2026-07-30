import { Component, effect, input } from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { PaymentDistribution } from '@domain/dashboard/models/dashboard.model';
import { TranslatePipe } from '@ngx-translate/core';
import { CHART_COLORS } from '@shared/utils/colors';

@Component({
  selector: 'app-payment-chart',
  imports: [NgxEchartsDirective, NzIconModule, TranslatePipe],
  templateUrl: './payment-chart.component.html',
})
export class PaymentChartComponent {
  payments = input<PaymentDistribution[]>([]);
  options!: EChartsCoreOption;

  constructor() {
    effect(() => {
      const payments = this.payments();
      const chartData = payments.map(p => ({ name: p.method, value: p.amount }));
      this.loadChartData(chartData);
    });
  }

  loadChartData(data: { name: string; value: number }[]) {
    this.options = {
      tooltip: {
        trigger: 'item',
      },
      legend: { bottom: '0%', left: 'center', icon: 'circle' },
      series: [
        {
          name: 'Payment Type',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '45%'],
          color: [CHART_COLORS.primary(), CHART_COLORS.info(), CHART_COLORS.warning()],
          itemStyle: { borderRadius: 2, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: data,
        },
      ],
      emphasis: {
        label: { show: true, fontSize: 12, fontWeight: 'bold' },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)',
        },
      },
    };
  }
}
