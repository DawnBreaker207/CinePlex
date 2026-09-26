import {
  Component,
  DestroyRef,
  computed,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HEADER_COLUMNS } from '@core/constants/column';
import {
  NzTableModule,
  NzTableQueryParams,
  NzTbodyComponent,
} from 'ng-zorro-antd/table';
import { VoucherStore } from '@domain/voucher/data-access/voucher.store';
import { DatePipe } from '@angular/common';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzOptionComponent, NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpaceComponent } from 'ng-zorro-antd/space';
import { NzWaveDirective } from 'ng-zorro-antd/core/wave';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { NzModalService } from 'ng-zorro-antd/modal';
import { FormVoucherComponent } from '@features/admin/voucher/components/voucher-form/voucher-form.component';
import { NzProgressComponent } from 'ng-zorro-antd/progress';
import { ICON_COLORS, CHART_COLORS } from '@shared/utils/colors';
import { confirmDelete } from '@shared/utils/confirm-delete';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { VoucherStatusPipe } from '@shared/pipes/voucher-status.pipe';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { Mode } from '@core/constants/enum';

@Component({
  selector: 'app-voucher-list',
  imports: [
    DatePipe,
    FormsModule,
    LoadingComponent,
    NzAlertComponent,
    NzButtonModule,
    NzIconDirective,
    NzInputModule,
    NzOptionComponent,
    NzSelectModule,
    NzSpaceComponent,
    NzTableModule,
    NzTbodyComponent,
    NzWaveDirective,
    TranslatePipe,
    NzProgressComponent,
    CurrencyFormatPipe,
    VoucherStatusPipe,
    NzTagModule,
    NzToolTipModule,
  ],
  templateUrl: './voucher-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class VoucherListComponent implements OnInit {
  readonly voucherStore = inject(VoucherStore);
  private modalService = inject(NzModalService);
  private destroyRef = inject(DestroyRef);
private translate = inject(TranslateService);
  headerColumn = HEADER_COLUMNS.VOUCHER;
  readonly ICON_COLORS = ICON_COLORS;
  readonly chartColors = CHART_COLORS;
  pageSize = 10;

  readonly searchText = signal('');
  readonly statusFilter = signal<string | null>(null);
  readonly sortOrder = signal<'newest' | 'oldest'>('newest');

  // Voucher catalog is small (admin-created campaigns): load once, filter locally.
  readonly filteredVouchers = computed(() => {
    const q = this.searchText().toLowerCase().trim();
    const status = this.statusFilter();
    let list = this.voucherStore.vouchers();
    if (q) {
      list = list.filter(
        (v) =>
          v.name.toLowerCase().includes(q) || v.code.toLowerCase().includes(q),
      );
    }
    if (status === 'PENDING') {
      list = list.filter((v) => v.status === 'PENDING_APPROVAL');
    } else if (status === 'true' || status === 'false') {
      const active = status === 'true';
      list = list.filter((v) => v.isActive === active);
    }
    return [...list].sort((a, b) =>
      this.sortOrder() === 'newest' ? b.id - a.id : a.id - b.id,
    );
  });

  ngOnInit() {
    this.loadVouchers();
  }

  loadVouchers() {
    this.voucherStore.loadVouchers({ page: 0, size: 200 });
  }

  openModal(mode: Mode, id?: number) {
    const modal = this.modalService.create({
      nzContent: FormVoucherComponent,
      nzTitle: undefined,
      nzClosable: true,
      nzWidth: 900,
      nzKeyboard: true,
      nzData: {
        mode: mode,
        id: id,
      },
      nzFooter: undefined,
    });

    modal.afterClose.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      if (result) {
        this.loadVouchers();
      }
    });
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    this.pageSize = params.pageSize;
  }

  isExpired(endDate: Date | string) {
    return new Date(endDate).getTime() < new Date().getTime();
  }

  onApprove(id: number) {
    this.modalService.confirm({
      nzTitle: this.translate.instant('voucher.approve'),
      nzContent: this.translate.instant('voucher.approveConfirm'),
      nzOkText: this.translate.instant('common.confirm'),
      nzCancelText: this.translate.instant('common.cancel'),
      nzOnOk: () => {
        this.voucherStore.approveVoucher(id);
      },
    });
  }
  onDelete(id: number) {
    confirmDelete(this.modalService, this.translate, 'voucher.deleteTitle', () =>
      this.voucherStore.deleteVoucher(id),
    );
  }
}
