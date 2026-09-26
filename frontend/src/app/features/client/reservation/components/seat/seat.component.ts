import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { SeatIconComponent } from '@shared/components/seat/seat.component';
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
  // Presentational — ReservationComponent owns seat-map init.
  readonly seatStore = inject(SeatStore);

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
        return `${base} text-primary cursor-pointer scale-110 drop-shadow-sm`;
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
