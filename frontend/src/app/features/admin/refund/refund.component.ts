import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzModalService } from 'ng-zorro-antd/modal';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { RefundStore } from '@domain/payment/data-access/refund.store';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-refund',
  imports: [
    DatePipe,
    LoadingComponent,
    NzAlertComponent,
    NzButtonModule,
    NzIconDirective,
    NzTagModule,
    NzToolTipModule,
    TranslatePipe,
    CurrencyFormatPipe,
  ],
  templateUrl: './refund.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class RefundComponent implements OnInit {
  readonly store = inject(RefundStore);
  private modalService = inject(NzModalService);
  private translate = inject(TranslateService);

  ngOnInit() {
    this.store.loadRefunds();
  }

  onApprove(reservationId: string) {
    this.modalService.confirm({
      nzTitle: this.translate.instant('refund.approve'),
      nzContent: this.translate.instant('refund.approveConfirm'),
      nzOkText: this.translate.instant('common.confirm'),
      nzCancelText: this.translate.instant('common.cancel'),
      nzOnOk: () => this.store.approveRefund(reservationId),
    });
  }

  onReject(reservationId: string) {
    this.modalService.confirm({
      nzTitle: this.translate.instant('refund.reject'),
      nzContent: this.translate.instant('refund.rejectConfirm'),
      nzOkText: this.translate.instant('common.confirm'),
      nzOkDanger: true,
      nzCancelText: this.translate.instant('common.cancel'),
      nzOnOk: () => this.store.rejectRefund(reservationId),
    });
  }
}