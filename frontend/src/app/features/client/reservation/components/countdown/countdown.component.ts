import {
  Component,
  effect,
  inject,
  OnInit,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { StorageService } from '@core/services/storage/storage.service';
import { ReservationStore } from '@features/client/reservation/reservation.store';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-countdown',
  imports: [NzToolTipModule, TranslatePipe, NgClass],
  templateUrl: './countdown.component.html',
  styleUrl: './countdown.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountdownComponent implements OnInit {
  readonly reservationStore = inject(ReservationStore);
  private storageService = inject(StorageService);
  private translate = inject(TranslateService);
  // Screen readers get threshold announcements only — never the ticking clock.
  readonly announceMsg = signal('');

  readonly isUrgent = computed(() => {
    const ttl = this.reservationStore.currentTTL()?.ttl ?? 999;
    return ttl <= 60 && ttl > 0;
  });

  constructor() {
    effect(() => {
      const ttl = this.reservationStore.currentTTL()?.ttl ?? -1;
      if (ttl === 60 || ttl === 10) {
        this.announceMsg.set(
          this.translate.instant('reservation.countdownWarn', { seconds: ttl }),
        );
      }
    });
  }

  ngOnInit() {
    const ttl = this.reservationStore.currentTTL()?.ttl;
    if (!ttl) {
      const state = this.storageService.getItem<{ ttl: number }>('reservationState');
      if (state && state.ttl > 0) {
        this.reservationStore.startCountdown(state.ttl);
      }
    }
  }
}
