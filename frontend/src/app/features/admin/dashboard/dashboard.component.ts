import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzCardComponent } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { DecimalPipe } from '@angular/common';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { DashboardItemComponent } from '@features/admin/dashboard/components/dashboard-item/dashboard-item.component';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { MovieTableComponent } from '@features/admin/dashboard/components/movie-table/movie-table.component';
import { TheaterTableComponent } from '@features/admin/dashboard/components/theater-table/theater-table.component';
import { PaymentChartComponent } from '@features/admin/dashboard/components/payment-chart/payment-chart.component';
import { TheaterChartComponent } from '@features/admin/dashboard/components/theater-chart/theater-chart.component';
import { MovieChartComponent } from '@features/admin/dashboard/components/movie-chart/movie-chart.component';
import { RevenueChartComponent } from '@features/admin/dashboard/components/revenue-chart/revenue-chart.component';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { ReportService } from '@core/services/report/report.service';
import { TranslatePipe } from '@ngx-translate/core';
import {
  DashboardState,
  DashboardStore,
} from '@features/admin/dashboard/dashboard.store';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    NzLayoutModule,
    NzSelectModule,
    NzSwitchModule,
    NzCardComponent,
    NzGridModule,
    NzIconModule,
    NzButtonModule,
    NzDatePickerModule,
    RevenueChartComponent,
    MovieChartComponent,
    TheaterChartComponent,
    PaymentChartComponent,
    TheaterTableComponent,
    MovieTableComponent,
    CurrencyFormatPipe,
    FormsModule,
    DecimalPipe,
    NzDropDownModule,
    NzMenuModule,
    DashboardItemComponent,
    LoadingComponent,
    TranslatePipe,
    NzAlertModule,
  ],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  readonly dashboardStore = inject(DashboardStore);
  private reportService = inject(ReportService);

  quarters = [
    { label: 'Quý 1 (Q1)', value: 1 },
    { label: 'Quý 2 (Q2)', value: 2 },
    { label: 'Quý 3 (Q3)', value: 3 },
    { label: 'Quý 4 (Q4)', value: 4 },
  ];

  years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i);

  onFilterChange(key: keyof DashboardState, value: any) {
    this.dashboardStore.updateFilter(key, value);
  }

  refreshData() {
    this.dashboardStore.loadSummary();
  }

  async exportReport(type: 'pdf' | 'excel') {
    const { saveAs } = await import('file-saver');
    this.reportService.downloadReport(type).subscribe({
      next: (res) => {
        const ext = type === 'excel' ? 'xlsx' : type;
        saveAs(res, `report.${ext}`);
      },
      error: () => {
        this.dashboardStore.setError('Xuất báo cáo thất bại. Vui lòng thử lại.');
      },
    });
  }
}
