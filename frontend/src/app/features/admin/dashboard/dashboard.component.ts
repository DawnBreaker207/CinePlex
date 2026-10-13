import {
  Component,
  DestroyRef,
  computed,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { DashboardItemComponent } from '@features/admin/dashboard/components/dashboard-item/dashboard-item.component';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { MovieTableComponent } from '@features/admin/dashboard/components/movie-table/movie-table.component';
import { TheaterTableComponent } from '@features/admin/dashboard/components/theater-table/theater-table.component';
import { OperationsComponent } from '@features/admin/dashboard/components/operations/operations.component';
import { PaymentChartComponent } from '@features/admin/dashboard/components/payment-chart/payment-chart.component';
import { TheaterChartComponent } from '@features/admin/dashboard/components/theater-chart/theater-chart.component';
import { MovieChartComponent } from '@features/admin/dashboard/components/movie-chart/movie-chart.component';
import { RevenueChartComponent } from '@features/admin/dashboard/components/revenue-chart/revenue-chart.component';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { SettingsService } from '@core/services/settings/settings.service';
import { NzModalService } from 'ng-zorro-antd/modal';
import { ToastService } from '@core/services/toast/toast.service';
import { ReportService } from '@core/services/report/report.service';
import { PaymentService } from '@domain/payment/data-access/payment.service';
import {
  DashboardMetrics,
  OperationAction,
} from '@domain/dashboard/models/dashboard.model';
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
    TranslatePipe,
    NzAlertModule,
    NzTabsModule,
  NzSpinModule,
    OperationsComponent,
  ],
  templateUrl: './dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class DashboardComponent {
  readonly dashboardStore = inject(DashboardStore);
  private reportService = inject(ReportService);
  private paymentService = inject(PaymentService);
  private modalService = inject(NzModalService);
  private toast = inject(ToastService);
  private destroyRef = inject(DestroyRef);
  private translate = inject(TranslateService);
  private settings = inject(SettingsService);

  isExporting = false;

  // computed over settings.lang() so quarter labels re-translate live.
  readonly quarters = computed(() => {
    void this.settings.lang();
    return [1, 2, 3, 4].map((q) => ({
      label: this.translate.instant(`admin.dashboard.q${q}`),
      value: q,
    }));
  });

  years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i);

  readonly granularity = computed(() =>
    this.dashboardStore.filterType() === 'quarter' ||
    this.dashboardStore.filterType() === 'year'
      ? 'week'
      : 'day',
  );

  trendPercent(key: keyof DashboardMetrics): number | null {
    const current = this.dashboardStore.metrics()?.[key];
    const previous = this.dashboardStore.previousMetrics()?.[key];
    if (
      current === undefined ||
      previous === undefined ||
      previous === null ||
      previous === 0 ||
      current === null
    ) {
      return null;
    }
    return ((current - previous) / previous) * 100;
  }

  onFilterChange(key: keyof DashboardState, value: DashboardState[keyof DashboardState]) {
    this.dashboardStore.updateFilter(key, value);
  }

  refreshData() {
    this.dashboardStore.loadSummary();
  }

  resetFilters() {
    this.dashboardStore.resetFilters();
  }

  resolveOperation(action: OperationAction) {
    const reload = () => this.dashboardStore.loadOperations();
    if (action.action === 'retry') {
      this.paymentService
        .reCheck(action.method ?? 'UNKNOWN', action.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (status) => {
            const paid = status === 'PAID';
            this.toast.createNotification({
              type: paid ? 'success' : 'warning',
              title: this.translate.instant('admin.dashboard.operations.retry'),
              message: this.translate.instant(
                paid
                  ? 'admin.dashboard.operations.retryPaid'
                  : 'admin.dashboard.operations.retryStillFailed',
              ),
            });
            reload();
          },
        });
      return;
    }
    const isApprove = action.action === 'approve';
    this.modalService.confirm({
      nzTitle: this.translate.instant(
        isApprove ? 'refund.approve' : 'refund.reject',
      ),
      nzContent: this.translate.instant(
        isApprove ? 'refund.approveConfirm' : 'refund.rejectConfirm',
      ),
      nzOkText: this.translate.instant('common.confirm'),
      nzOkDanger: !isApprove,
      nzCancelText: this.translate.instant('common.cancel'),
      nzOnOk: () => {
        const call = isApprove
          ? this.paymentService.approveRefund(action.id)
          : this.paymentService.rejectRefund(action.id);
        call.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => {
            this.toast.createNotification({
              type: 'success',
              title: this.translate.instant(
                isApprove ? 'refund.approve' : 'refund.reject',
              ),
              message: this.translate.instant(
                isApprove ? 'refund.approved' : 'refund.rejected',
              ),
            });
            reload();
          },
        });
      },
    });
  }

  async exportReport(type: 'pdf' | 'excel') {
    if (this.isExporting) return;
    this.isExporting = true;
    const { saveAs } = await import('file-saver');
    this.reportService
      .downloadReport(type)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const ext = type === 'excel' ? 'xlsx' : type;
          saveAs(res, `report.${ext}`);
        },
        error: () => {
          this.dashboardStore.setError(
            this.translate.instant('admin.dashboard.exportFailed'),
          );
        },
        complete: () => {
          this.isExporting = false;
        },
      });
  }
}
