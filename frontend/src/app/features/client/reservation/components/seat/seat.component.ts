import {
  Component,
  effect,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
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
  changeDetection: ChangeDetectionStrategy.OnPush,
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

  toggleCell(seats: Seat[]) {
    this.seatStore.toggleSeats(seats);
  }

  isCellDisabled(seats: Seat[]): boolean {
    return seats.some((s) => s.status === 'BOOKED' || s.status === 'HOLD');
  }

  getSeatClass(seat: Seat): string {
    const base = 'transition-colors duration-200 ease-in-out';
    switch (seat.status) {
      case 'BOOKED':
        return `${base} text-danger cursor-not-allowed`;
      case 'HOLD':
        return `${base} text-warning cursor-not-allowed`;
      case 'SELECTED':
        return `${base} text-success cursor-pointer scale-110 drop-shadow-sm`;
      default:
        // Color carries status only; the icon shape carries the seat type.
        return `${base} text-muted cursor-pointer hover:text-success hover:scale-105`;
    }
  }

  // Shape carries the type in every status: couple units render the
  // long bench, VIP seats the full chair, everything else pinched single.
  iconVariant(cell: { span: number }, seat: Seat): 'single' | 'couple' | 'vip' {
    if (cell.span > 1) return 'couple';
    return seat.seatType === 'VIP' ? 'vip' : 'single';
  }
}
