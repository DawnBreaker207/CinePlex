import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { Subject } from 'rxjs';
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
})
export class FormReservationComponent implements OnInit, OnDestroy {
  private modelRef = inject(NzModalRef);
  destroy$: Subject<void> = new Subject();
  readonly reservationStore = inject(ReservationStore);

  ngOnInit() {
    const { reservationId } = this.modelRef.getConfig().nzData;
    this.reservationStore.loadReservation({ id: reservationId });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
