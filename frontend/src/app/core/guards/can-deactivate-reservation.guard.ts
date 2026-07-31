import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { NzModalService } from 'ng-zorro-antd/modal';
import { TranslateService } from '@ngx-translate/core';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';

export const canDeactivateReservation: CanDeactivateFn<any> = () => {
  const seatStore = inject(SeatStore);
  if (seatStore.selectedSeats().length === 0) return true;

  const modalService = inject(NzModalService);
  const translate = inject(TranslateService);
  return new Promise<boolean>((resolve) => {
    modalService.confirm({
      nzTitle: translate.instant('reservation.leaveTitle'),
      nzContent: translate.instant('reservation.leaveContent'),
      nzOkText: translate.instant('reservation.leave'),
      nzOkDanger: true,
      nzCancelText: translate.instant('reservation.stay'),
      nzOnOk: () => resolve(true),
      nzOnCancel: () => resolve(false),
    });
  });
};
