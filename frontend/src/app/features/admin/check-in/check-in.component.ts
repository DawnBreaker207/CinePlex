import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ReservationService } from '@domain/reservation/data-access/reservation.service';
import { Reservation } from '@domain/reservation/models/reservation.model';
import { TicketStubComponent } from '@shared/components/ticket-stub/ticket-stub.component';
import { StatusTagsPipe } from '@shared/pipes/status-tags.pipe';

@Component({
  selector: 'app-check-in',
  imports: [
    FormsModule,
    NzButtonModule,
    NzInputModule,
    NzIconModule,
    NzTagModule,
    NzResultModule,
    NzSpinModule,
    TranslatePipe,
    TicketStubComponent,
    StatusTagsPipe,
  ],
  templateUrl: './check-in.component.html',
  styleUrls: ['./check-in.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckInComponent {
  private reservationService = inject(ReservationService);
  private translate = inject(TranslateService);

  code = '';
  loading = signal(false);
  error = signal<string | null>(null);
  reservation = signal<Reservation | null>(null);
  checkedIn = signal(false);

  search() {
    const id = this.code.trim();
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.reservationService.getReservation(id).subscribe({
      next: (res) => {
        this.reservation.set(res);
        this.checkedIn.set(res.reservationStatus === 'CONFIRMED');
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('checkin.notFound'));
        this.loading.set(false);
      },
    });
  }

  confirm() {
    this.checkedIn.set(true);
  }

  isCheckedIn() {
    return this.checkedIn();
  }

  ticketMeta(res: Reservation) {
    const t = this.translate.instant.bind(this.translate);
    return [
      {
        label: t('checkin.theater'),
        value: `${res.showtime.theaterName} • ${res.showtime.theaterLocation}`,
      },
      {
        label: t('checkin.showtime'),
        value: `${res.showtime.showDate.toLocaleDateString('vi-VN')} ${res.showtime.showTime.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`,
        accent: true,
      },
      { label: t('checkin.customer'), value: res.user.username },
      {
        label: t('checkin.seats'),
        value: res.seats.map((s) => s.seatNumber).join(', '),
      },
    ];
  }
}
