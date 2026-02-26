import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { ActivatedRoute, Router } from '@angular/router';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { Store } from '@ngrx/store';
import { StorageService } from '@core/services/storage/storage.service';
import { ReservationRequest } from '@domain/reservation/models/reservation.model';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { TranslatePipe } from '@ngx-translate/core';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-payment-result',
  imports: [NzResultModule, NzButtonModule, NzSpinModule, TranslatePipe],
  templateUrl: './payment-result.component.html',
  styleUrl: './payment-result.component.css',
})
export class PaymentResultComponent implements OnInit, OnDestroy {
  private store = inject(Store);
  private storageService = inject(StorageService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly reservationStore = inject(ReservationStore);
  readonly params = toSignal(this.route.queryParams);

  status = signal<'success' | 'error' | 'info'>('info');
  errorMessage = signal<string>('');
  countdown = signal(10);
  reservationCode = signal<string>('');

  private intervalId: any;

  ngOnInit() {
    const state =
      this.storageService.getItem<ReservationRequest>('reservationState');
    const query = this.params();

    if (!state || !query) {
      this.status.set('error');
      this.errorMessage.set(`Không tìm thấy thông tin phiên giao dịch`);
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
    } else {
      this.status.set('error');
      this.errorMessage.set(`Thanh toán thất bại`);
      this.reservationStore.cancel();
    }
    this.storageService.removeItem('reservationState');
    this.startCountDown();
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
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
    this.reservationStore.stopCountdown();
  }
}
