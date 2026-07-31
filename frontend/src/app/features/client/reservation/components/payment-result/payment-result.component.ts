import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { ActivatedRoute, Router } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { Store } from '@ngrx/store';
import { StorageService } from '@core/services/storage/storage.service';
import { ReservationRequest } from '@domain/reservation/models/reservation.model';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { toSignal } from '@angular/core/rxjs-interop';
import { TicketStubComponent } from '@shared/components/ticket-stub/ticket-stub.component';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-payment-result',
  imports: [
    NzButtonModule,
    NzIconModule,
    TranslatePipe,
    TicketStubComponent,
  ],
  templateUrl: './payment-result.component.html',
  styleUrl: './payment-result.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentResultComponent implements OnInit {
  private store = inject(Store);
  private storageService = inject(StorageService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private datePipe = inject(DatePipe);
private translate = inject(TranslateService);

  readonly reservationStore = inject(ReservationStore);
  readonly params = toSignal(this.route.queryParams);
  readonly showtime = toSignal(this.store.select(selectSelectedShowtime));

  status = signal<'success' | 'error' | 'info'>('info');
  errorMessage = signal<string>('');
  countdown = signal(10);
  reservationCode = signal<string>('');

  private intervalId: ReturnType<typeof setInterval> | null = null;

  ngOnInit() {
    const state =
      this.storageService.getItem<ReservationRequest>('reservationState');
    const query = this.params();

    if (!state || !query) {
      this.status.set('error');
      this.errorMessage.set(
        this.translate.instant('reservation.sessionNotFound'),
      );
      this.startCountDown();
      return;
    }
    this.store.dispatch(ShowtimeActions.loadShowtime({ id: state.showtimeId }));

    const status = query['status'];
    const reservationCode = query['reservationId'];
    this.reservationCode.set(reservationCode || '');

    if (reservationCode && status === 'success') {
      this.status.set('success');
      this.reservationStore.confirm(reservationCode);
      this.storageService.removeItem('reservationState');
    } else {
      this.status.set('error');
      this.errorMessage.set(
        this.translate.instant('reservation.paymentFailed'),
      );
      this.reservationStore.cancel();
    }
    this.startCountDown();
  }

  retryPayment() {
    const state = this.storageService.getItem<ReservationRequest>('reservationState');
    if (state?.showtimeId) {
      this.router.navigate([
        '/reservation',
        state.reservationId,
        state.showtimeId,
      ]);
    } else {
      this.router.navigate(['/home']);
    }
  }

  goHome() {
    if (this.intervalId) clearInterval(this.intervalId);
    this.router.navigate(['/home']);
  }

  startCountDown() {
    if (this.intervalId) clearInterval(this.intervalId);
    this.intervalId = setInterval(() => {
      this.countdown.update((val) => val - 1);
      if (this.countdown() <= 0) {
        this.goHome();
      }
    }, 1000);
    this.destroyRef.onDestroy(() => {
      if (this.intervalId) clearInterval(this.intervalId);
      this.reservationStore.stopCountdown();
    });
  }

  reservationMeta() {
    const st = this.showtime();
    if (!st) return [];
    const t = this.translate.instant.bind(this.translate);
    return [
      { label: t('checkin.movieTitle'), value: st.movieTitle },
      {
        label: t('booking.showDate'),
        value: this.datePipe?.transform(st.showDate, 'dd/MM/yyyy') ?? '',
      },
      { label: t('booking.showtime'), value: st.showTime.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }), accent: true },
      { label: t('checkin.theater'), value: st.theaterName },
    ];
  }
}
