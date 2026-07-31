import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzWaveModule } from 'ng-zorro-antd/core/wave';
import { DatePipe } from '@angular/common';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzQRCodeModule } from 'ng-zorro-antd/qr-code';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { ReservationProfile } from '@domain/reservation/models/reservation.model';
import { BarcodeComponent } from '@shared/components/barcode/barcode.component';
import { TranslatePipe } from '@ngx-translate/core';

export interface MovieTicket {
  id: string;
  title: string;
  poster: string;
  cinema: string;
  room: string;
  date: string;
  time: string;
  seats: string[];
  price: number;
  status: 'Upcoming' | 'Completed' | 'Cancelled';
}

@Component({
  selector: 'app-booking-history',
  imports: [
    NzButtonModule,
    NzEmptyModule,
    NzIconModule,
    NzWaveModule,
    NzModalModule,
    NzQRCodeModule,
    NzTagModule,
    DatePipe,
    CurrencyFormatPipe,
    BarcodeComponent,
    TranslatePipe,
  ],
  templateUrl: './booking-history.component.html',
  styleUrl: './booking-history.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingHistoryComponent {
  tickets = input<ReservationProfile[]>([]);
  isDetailVisible: boolean = false;
  selectedTicket: ReservationProfile | null = null;

  openTicketDetail(ticket: ReservationProfile) {
    this.selectedTicket = ticket;
    this.isDetailVisible = true;
  }

  closeTicketDetail() {
    this.isDetailVisible = false;
  }
}
