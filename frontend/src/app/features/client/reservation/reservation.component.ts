import {
  Component,
  computed,
  effect,
  HostListener,
  inject,
  input,
  OnInit,
  signal,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzStepsModule } from 'ng-zorro-antd/steps';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzModalService } from 'ng-zorro-antd/modal';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { StorageService } from '@core/services/storage/storage.service';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { selectJwt } from '@core/auth/auth.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { SeatComponent } from '@features/client/reservation/components/seat/seat.component';
import { DetailFilmComponent } from '@features/client/reservation/components/detail-film/detail-film.component';
import { CurrencyPipe } from '@angular/common';
import { ConfirmComponent } from '@features/client/reservation/components/confirm/confirm.component';
import { ComboComponent, ComboSelection } from '@features/client/reservation/components/combo/combo.component';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { ReservationRequest } from '@domain/reservation/models/reservation.model';
import { selectSelectedTheater } from '@domain/theater/data-access/theater.selectors';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

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
    ConfirmComponent,
    ComboComponent,
    TranslatePipe,
    CurrencyPipe,
    ],
  templateUrl: './reservation.component.html',
  styleUrl: './reservation.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class ReservationComponent implements OnInit {
  confirmComponent = viewChild.required(ConfirmComponent);

  private store = inject(Store);
  private router = inject(Router);
  private modalService = inject(NzModalService);
  readonly seatStore = inject(SeatStore);
  readonly reservationStore = inject(ReservationStore);
  private storageService = inject(StorageService);
  private translate = inject(TranslateService);

  reservationId = input<string>('');
  showtimeId = input<string>('');

  readonly showtime = this.store.selectSignal(selectSelectedShowtime);
  readonly user = this.store.selectSignal(selectJwt);
  readonly theater = this.store.selectSignal(selectSelectedTheater);
  index = signal(0);
  steps = signal([0, 1, 2]);
  readonly combos = signal<ComboSelection[]>([]);
  readonly comboTotal = computed(() =>
    this.combos().reduce((sum, c) => sum + c.price * c.qty, 0),
  );

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent) {
    if (this.seatStore.selectedSeats().length > 0) {
      event.preventDefault();
    }
  }

  constructor() {
    effect(() => {
      if (
        this.reservationStore.holdingSeats() &&
        !this.reservationStore.saving()
      ) {
        this.reservationStore.resetHolding();
        if (!this.reservationStore.error()) {
          this.index.set(1);
        }
      }
    });
  }

  ngOnInit() {
    const state = this.storageService.getItem<ReservationRequest & { theaterId: number }>('reservationState');
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

  onStepChange(newIndex: number) {
    const currentIndex = this.index();
    if (newIndex < currentIndex) {
      this.index.set(newIndex);
    }
  }

  onComboNext() {
    this.index.set(2);
  }

  onConfirm() {
    if (this.reservationStore.holdingSeats()) return;
    this.reservationStore.holdSeats();
  }

  onPay() {
    if (this.reservationStore.submittingPayment()) return;
    const seatsTotal = this.seatStore.totalPrice();
    const grandTotal = seatsTotal + this.comboTotal();
    this.modalService.confirm({
      nzTitle: this.translate.instant('reservation.payTitle'),
      nzContent: this.translate.instant('reservation.payContent', {
        amount: grandTotal.toLocaleString('vi-VN'),
        count: this.seatStore.selectSeatsCount(),
      }),
      nzOkText: this.translate.instant('reservation.payNow'),
      nzCancelText: this.translate.instant('common.cancel'),
      nzOnOk: () => {
        this.reservationStore.pay(
          this.confirmComponent().radioValue,
          this.confirmComponent().appliedVoucher()?.code || undefined,
          this.comboTotal(),
        );
      },
    });
  }
}
