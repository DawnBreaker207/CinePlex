import { Component, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { StorageService } from '@core/services/storage/storage.service';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { SeatIconComponent } from '@shared/components/seat/seat.component';
import { ReservationRequest } from '@domain/reservation/models/reservation.model';
import { Seat } from '@domain/seat/models/seat.model';
import { TranslatePipe } from '@ngx-translate/core';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';

@Component({
  selector: 'app-seat',
  imports: [CommonModule, NzIconModule, SeatIconComponent, TranslatePipe],
  templateUrl: './seat.component.html',
  styleUrl: './seat.component.css',
})
export class SeatComponent {
  private store = inject(Store);
  private storageService = inject(StorageService);
  showtime = this.store.selectSignal(selectSelectedShowtime);
  readonly seatStore = inject(SeatStore);

  constructor() {
    effect(() => {
      const currentShowtime = this.showtime();
      const reservationState =
        this.storageService.getItem<ReservationRequest>('reservationState');

      if (currentShowtime) {
        this.seatStore.initSeatMap({
          showtimeId: currentShowtime.id,
          userId: reservationState?.userId as number,
          reservationId: reservationState?.reservationId || null,
        });
      }
    });
  }

  toggleSeat(seat: Seat) {
    this.seatStore.toggleSeat(seat);
  }

  getSeatClass(seat: Seat): string {
    const base = 'transition-colors  duration-200 ease-in-out';
    switch (seat.status) {
      case 'BOOKED':
        return `${base} text-red-500 cursor-not-allowed`;
      case 'HOLD':
        return `${base} text-orange-500 cursor-not-allowed`;
      case 'SELECTED':
        return `${base} text-blue-500 cursor-pointer scale-110 drop-shadow-sm`;
      default:
        return `${base} text-gray-400 cursor-pointer hover:text-green-500 hover:scale-105`;
    }
  }
}
