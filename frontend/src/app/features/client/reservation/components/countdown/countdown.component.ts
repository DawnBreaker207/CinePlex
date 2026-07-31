import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from '@angular/core';
import { StorageService } from '@core/services/storage/storage.service';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-countdown',
  imports: [NzToolTipModule, TranslatePipe],
  templateUrl: './countdown.component.html',
  styleUrl: './countdown.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountdownComponent implements OnInit {
  readonly reservationStore = inject(ReservationStore);
  private storageService = inject(StorageService);

  ngOnInit() {
    const ttl = this.reservationStore.currentTTL()?.ttl;
    if (!ttl) {
      const state = this.storageService.getItem<any>('reservationState');
      if (state && state.ttl > 0) {
        this.reservationStore.startCountdown(state.ttl);
      }
    }
  }
}
