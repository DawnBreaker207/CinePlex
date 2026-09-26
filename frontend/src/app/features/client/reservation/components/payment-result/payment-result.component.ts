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
import { formatDate } from '@angular/common';
import { PaymentService } from '@domain/payment/data-access/payment.service';
import { ToastService } from '@core/services/toast/toast.service';
import { timeFormat } from '@shared/utils/date.helper';

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
private translate = inject(TranslateService);

  readonly reservationStore = inject(ReservationStore);
  private paymentService = inject(PaymentService);
  private toast = inject(ToastService);
  readonly showtime = toSignal(this.store.select(selectSelectedShowtime));

  status = signal<'success' | 'error' | 'info'>('info');
  errorMessage = signal<string>('');
  countdown = signal(10);
  reservationCode = signal<string>('');
  autoRedirect = signal(true);
  rechecking = signal(false);

  private intervalId: ReturnType<typeof setInterval> | null = null;

  ngOnInit() {
    const state =
      this.storageService.getItem<ReservationRequest>('reservationState');
    const query = this.route.snapshot.queryParams ?? {};

    const status = query['status'];
    const reservationCode = query['reservationId'];
    this.reservationCode.set(reservationCode || '');

    if (!state && reservationCode) {
      // S2: landed via URL/bookmark without SPA state — resolve from server.
      this.recheck();
    }
    if (!state || !query) {
      this.status.set('error');
      this.errorMessage.set(
        this.translate.instant('reservation.sessionNotFound'),
      );
      this.startCountDown();
      return;
    }
    this.store.dispatch(ShowtimeActions.loadShowtime({ id: state.showtimeId }));

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
    this.startPoll();
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

  cancelAutoRedirect() {
    this.autoRedirect.set(false);
    if (this.intervalId) clearInterval(this.intervalId);
  }

  // S2: manual button stays verbose; the auto-poll is quiet (only toasts
  // on a real PAID transition, never spams "still pending").
  recheck(quiet = false) {
    const code = this.reservationCode();
    const last = this.storageService.getItem<{
      provider: string;
      reservationId: string;
    }>('lastPayment');
    if (!code || !last || this.rechecking()) return;
    this.rechecking.set(true);
    this.paymentService
      .reCheck(last.provider.toLowerCase(), last.reservationId)
      .subscribe({
        next: (state) => {
          this.rechecking.set(false);
          if (state === 'PAID') {
            this.stopPoll();
            this.applySuccess(code);
          } else if (!quiet) {
            this.toast.createNotification({
              type: 'info',
              title: this.translate.instant('reservation.recheck'),
              message: this.translate.instant('reservation.stillPending'),
              position: 'top',
            });
          }
        },
        error: () => this.rechecking.set(false),
      });
  }

  private pollId: ReturnType<typeof setInterval> | null = null;

  private stopPoll() {
    if (this.pollId) {
      clearInterval(this.pollId);
      this.pollId = null;
    }
  }

  private startPoll() {
    this.stopPoll();
    this.pollId = setInterval(() => {
      if (this.status() === 'success' || !this.reservationCode()) {
        this.stopPoll();
        return;
      }
      this.recheck(true);
    }, 4000);
    this.destroyRef.onDestroy(() => this.stopPoll());
  }

  private applySuccess(code: string) {
    this.status.set('success');
    this.errorMessage.set('');
    this.reservationStore.confirm(code);
    this.storageService.removeItem('reservationState');
    this.storageService.removeItem('lastPayment');
    this.startCountDown();
  }

  startCountDown() {
    if (this.intervalId) clearInterval(this.intervalId);
    this.intervalId = setInterval(() => {
      if (!this.autoRedirect()) return;
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
        value: st.showDate ? formatDate(st.showDate, 'dd/MM/yyyy', 'en-US') : '',
      },
      { label: t('booking.showtime'), value: timeFormat(st.showTime), accent: true },
      { label: t('checkin.theater'), value: st.theaterName },
    ];
  }
}
