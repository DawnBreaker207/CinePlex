import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { NzModalService } from 'ng-zorro-antd/modal';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';

export const canDeactivateReservation: CanDeactivateFn<any> = () => {
  const seatStore = inject(SeatStore);
  if (seatStore.selectedSeats().length === 0) return true;

  const modalService = inject(NzModalService);
  return new Promise<boolean>((resolve) => {
    modalService.confirm({
      nzTitle: 'Bạn có chắc muốn rời khỏi trang?',
      nzContent: 'Ghế đã chọn sẽ được giải phóng. Vui lòng hoàn tất đặt vé trước khi rời đi.',
      nzOkText: 'Rời khỏi',
      nzOkDanger: true,
      nzCancelText: 'Ở lại',
      nzOnOk: () => resolve(true),
      nzOnCancel: () => resolve(false),
    });
  });
};
