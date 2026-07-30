import { Component, DestroyRef, effect, HostListener, inject, input, OnInit, signal, viewChild } from '@angular/core';
import { PaymentMethod } from '@core/constants/enum';
import { NzStepsModule } from 'ng-zorro-antd/steps';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzModalService } from 'ng-zorro-antd/modal';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { CommonModule, DOCUMENT } from '@angular/common';
import { PaymentService } from '@domain/payment/data-access/payment.service';
import { StorageService } from '@core/services/storage/storage.service';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { selectJwt } from '@core/auth/auth.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { ReservationRequest } from '@domain/reservation/models/reservation.model';
import { SeatComponent } from '@features/client/reservation/components/seat/seat.component';
import { DetailFilmComponent } from '@features/client/reservation/components/detail-film/detail-film.component';
import { ConfirmComponent } from '@features/client/reservation/components/confirm/confirm.component';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { selectSelectedTheater } from '@domain/theater/data-access/theater.selectors';
import { environment } from '@env/environment';

@Component({
  selector: 'app-reservation',
  imports: [
    NzStepsModule,
    NzIconModule,
    NzAlertModule,
    NzButtonModule,
    NzLayoutModule,
    SeatComponent,
    DetailFilmComponent,
    CommonModule,
    ConfirmComponent,
  ],
  templateUrl: './reservation.component.html',
  styleUrl: './reservation.component.css',
  providers: [NzModalService],
})
export class ReservationComponent implements OnInit {
  confirmComponent = viewChild.required(ConfirmComponent);
  private document = inject(DOCUMENT);

  private store = inject(Store);
  private router = inject(Router);
  private modalService = inject(NzModalService);
  readonly seatStore = inject(SeatStore);
  readonly reservationStore = inject(ReservationStore);
  private paymentService = inject(PaymentService);
  private storageService = inject(StorageService);
  private destroyRef = inject(DestroyRef);

  reservationId = input<string>('');
  showtimeId = input<string>('');

  readonly showtime = this.store.selectSignal(selectSelectedShowtime);
  readonly user = this.store.selectSignal(selectJwt);
  readonly theater = this.store.selectSignal(selectSelectedTheater);
  index = signal(0);
  steps = signal([0, 1]);
  submittingPayment = signal(false);
  holdingSeats = signal(false);
  paymentError = signal<string | null>(null);

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent) {
    if (this.seatStore.selectedSeats().length > 0) {
      event.preventDefault();
    }
  }

  constructor() {
    effect(() => {
      if (this.holdingSeats() && !this.reservationStore.saving()) {
        this.holdingSeats.set(false);
        if (!this.reservationStore.error()) {
          this.index.set(1);
        }
      }
    });
  }

  ngOnInit() {
    const state = this.storageService.getItem<any>('reservationState');
    const routeId = this.reservationId();
    if (!state || (!state.reservationId && !routeId)) {
      this.router.navigate(['/home']);
      return;
    }

    const reservationId = routeId || state.reservationId;

    if (!this.reservationStore.reservationId() && reservationId) {
      this.reservationStore.restore({ reservationId: reservationId });
    }

    this.store.dispatch(TheaterActions.loadTheater({ id: state.theaterId }));
    this.store.dispatch(ShowtimeActions.loadShowtime({ id: state.showtimeId }));
  }

  private toPaymentMethod(value: string | undefined): PaymentMethod {
    switch (value) {
      case 'momo': return 'MOMO';
      case 'zalopay': return 'ZALOPAY';
      case 'cash': return 'CASH';
      case 'atm':
      case 'visa':
      case 'vnpay': return 'VNPAY';
      default: return 'VNPAY';
    }
  }

  onStepChange(newIndex: number) {
    const currentIndex = this.index();
    if (newIndex < currentIndex) {
      this.index.set(newIndex);
    }
  }

  onConfirm() {
    if (this.holdingSeats()) return;

    const selectedSeats = this.seatStore.selectedSeats();
    const user = this.user();
    const showtime = this.showtime();
    const theater = this.theater();
    const resId = this.reservationStore.reservationId();

    if (!selectedSeats.length || !user || !showtime || !resId || !theater) {
      return;
    }

    const payload: ReservationRequest = {
      reservationId: resId,
      userId: user.userId,
      showtimeId: showtime.id,
      seatIds: selectedSeats.map((s) => s.id),
    };

    this.storageService.setItem('reservationState', {
      ...payload,
      theaterId: theater.id,
    });
    this.holdingSeats.set(true);
    this.reservationStore.holdSeat({ reservation: payload });
  }

  onPay() {
    if (this.submittingPayment()) return;

    const resId = this.reservationStore.reservationId();
    const total = this.seatStore.totalPrice();
    const count = this.seatStore.selectSeatsCount();
    if (!resId || !total || !count) return;

    this.paymentError.set(null);

    this.modalService.confirm({
      nzTitle: 'Xác nhận thanh toán',
      nzContent: `Bạn sắp thanh toán ${total.toLocaleString('vi-VN')}₫ cho ${count} vé.`,
      nzOkText: 'Thanh toán ngay',
      nzCancelText: 'Hủy',
      nzOnOk: () => {
        this.submittingPayment.set(true);

        const doPayment = (paymentUrl: string) => {
          if (environment.useMock) {
            this.reservationStore.confirm(resId);
            this.storageService.removeItem('reservationState');
            this.submittingPayment.set(false);
            this.router.navigateByUrl(paymentUrl);
          } else {
              this.document.defaultView?.open(paymentUrl, '_self');
          }
        };

        this.paymentService
          .createPayment({
            reservationId: resId,
            amount: total,
            paymentType: this.toPaymentMethod(this.confirmComponent().radioValue),
          })
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: doPayment,
            error: (err) => {
              this.submittingPayment.set(false);
              this.paymentError.set(err?.message || 'Thanh toán thất bại. Vui lòng thử lại.');
            },
          });
      },
    });
  }
}
