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
import { ToastService } from '@core/services/toast/toast.service';
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
  private toast = inject(ToastService);

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
        this.checkedIn.set(res.checkedIn ?? res.reservationStatus === 'CONFIRMED');
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translate.instant('checkin.notFound'));
        this.loading.set(false);
      },
    });
  }

  confirm() {
    const res = this.reservation();
    if (!res || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.reservationService.checkIn(res.reservationCode || this.code.trim()).subscribe({
      next: (updated) => {
        this.reservation.set(updated);
        this.checkedIn.set(updated.checkedIn ?? true);
        this.loading.set(false);
        this.toast.createNotification({
          type: 'success',
          title: this.translate.instant('checkin.done'),
          message: this.translate.instant('checkin.doneSub'),
        });
      },
      error: (err) => {
        this.error.set(
          typeof err?.message === 'string' && err.message
            ? err.message
            : this.translate.instant('checkin.notFound'),
        );
        this.loading.set(false);
      },
    });
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
        value: `${new Date(res.showtime.showDate).toLocaleDateString('vi-VN')} ${new Date(res.showtime.showTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`,
        accent: true,
      },
      { label: t('checkin.customer'), value: res.user.username },
      {
        label: t('checkin.seats'),
        value: res.seats.join(', '),
      },
    ];
  }
}
