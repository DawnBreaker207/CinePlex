import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
} from '@angular/core';
import { NzImageModule } from 'ng-zorro-antd/image';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { NzWaveDirective } from 'ng-zorro-antd/core/wave';
import { Store } from '@ngrx/store';
import { CommonModule } from '@angular/common';
import { CountdownComponent } from '@features/client/reservation/components/countdown/countdown.component';
import { selectSelectedMovie } from '@domain/movie/data-access/movie.selectors';
import { selectSelectedTheater } from '@domain/theater/data-access/theater.selectors';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { timeFormat } from '@shared/utils/date.helper';
import { TranslatePipe } from '@ngx-translate/core';
import { SeatStore } from '@features/client/reservation/components/seat/seat.store';

@Component({
  selector: 'app-detail-film',
  imports: [
    NzImageModule,
    NzIconModule,
    NzButtonComponent,
    NzWaveDirective,
    CommonModule,
    CountdownComponent,
    TranslatePipe,
  ],
  templateUrl: './detail-film.component.html',
  styleUrl: './detail-film.component.css',
})
export class DetailFilmComponent {
  private store = inject(Store);
  readonly seatStore = inject(SeatStore);

  index = input<number>(0);
  steps = input<number[]>([]);
  paymentLoading = input(false);
  holdingSeats = input(false);
  stepChange = output<number>();
  confirm = output<void>();
  pay = output<void>();

  readonly movie = this.store.selectSignal(selectSelectedMovie);
  readonly theater = this.store.selectSignal(selectSelectedTheater);
  readonly showtime = this.store.selectSignal(selectSelectedShowtime);

  readonly selectedSeatNumbers = computed(() =>
    this.seatStore.selectedSeats().map((s) => s.seatNumber.replace(/\s+/g, '')),
  );

  constructor() {
    effect(() => {
      const movieId = this.showtime()?.movieId;
      if (movieId) {
        this.store.dispatch(MovieActions.loadMovie({ id: movieId as number }));
      }
    });
  }

  emitConfirm() {
    this.confirm.emit();
  }

  prevStep() {
    this.stepChange.emit(this.index() - 1);
  }

  emitPay() {
    this.pay.emit();
  }

  protected readonly timeFormat = timeFormat;
}
