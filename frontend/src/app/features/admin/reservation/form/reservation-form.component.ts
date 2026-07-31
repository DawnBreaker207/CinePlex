import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { NzTagComponent } from 'ng-zorro-antd/tag';
import { StatusTagsPipe } from '@shared/pipes/status-tags.pipe';
import { ReservationStore } from '@domain/reservation/data-access/reservation.store';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-form',
  imports: [
    DatePipe,
    CurrencyPipe,
    NzTagComponent,
    StatusTagsPipe,
    LoadingComponent,
    TranslatePipe,
  ],
  templateUrl: './reservation-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormReservationComponent implements OnInit {
  private modelRef = inject(NzModalRef);
  readonly reservationStore = inject(ReservationStore);

  ngOnInit() {
    const { reservationId } = this.modelRef.getConfig().nzData;
    this.reservationStore.loadReservation({ id: reservationId });
  }
}
