import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from '@angular/core';
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
import { NzTagModule } from 'ng-zorro-antd/tag';
import { Mode } from '@core/constants/enum';

@Component({
  selector: 'app-voucher-list',
  imports: [
    DatePipe,
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
    NzTagModule,
  ],
  templateUrl: './voucher-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class VoucherListComponent implements OnInit {
  readonly voucherStore = inject(VoucherStore);
  private modalService = inject(NzModalService);
private translate = inject(TranslateService);
  headerColumn = HEADER_COLUMNS.VOUCHER;
  readonly ICON_COLORS = ICON_COLORS;
  readonly chartColors = CHART_COLORS;
  pageIndex = 1;
  pageSize = 10;

  ngOnInit() {
    this.loadVouchers();
  }

  loadVouchers() {
    this.voucherStore.loadVouchers({
      page: this.pageIndex - 1,
      size: this.pageSize,
    });
  }

  openModal(mode: Mode, id?: number) {
    const modal = this.modalService.create({
      nzContent: FormVoucherComponent,
      nzTitle: undefined,
      nzClosable: true,
      nzWidth: 1200,
      nzKeyboard: true,
      nzData: {
        mode: mode,
        id: id,
      },
      nzFooter: undefined,
    });

    modal.afterClose.subscribe((result) => {
      if (result) {
        this.loadVouchers();
      }
    });
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    const { pageIndex, pageSize } = params;
    this.pageIndex = pageIndex;
    this.pageSize = pageSize;
    this.loadVouchers();
  }

  isExpired(endDate: Date | string) {
    return new Date(endDate).getTime() < new Date().getTime();
  }
  onDelete(id: number) {
    confirmDelete(this.modalService, this.translate, 'voucher.deleteTitle', () =>
      this.voucherStore.deleteVoucher(id),
    );
  }
}
