import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { CommonModule } from '@angular/common';
import {
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
} from '@angular/forms';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzFormLabelComponent } from 'ng-zorro-antd/form';
import { NzTooltipDirective } from 'ng-zorro-antd/tooltip';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { StatusTagsPipe } from '@shared/pipes/status-tags.pipe';
import { formatDate } from '@shared/utils/date.helper';
import { ReservationStatus } from '@core/constants/enum';
import { FormReservationComponent } from '@features/admin/reservation/form/reservation-form.component';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { ReservationStore } from '@domain/reservation/data-access/reservation.store';
import { TranslatePipe } from '@ngx-translate/core';
import { HEADER_COLUMNS, RESERVATION_STATUS } from '@core/constants/column';

@Component({
  selector: 'app-reservation',
  imports: [
    NzTableModule,
    NzButtonModule,
    NzInputModule,
    NzSelectModule,
    NzSpaceModule,
    NzIconModule,
    CommonModule,
    NzDatePickerModule,
    ReactiveFormsModule,
    NzTagModule,
    StatusTagsPipe,
    NzDropDownModule,
    NzAlertComponent,
    NzFormLabelComponent,
    NzTooltipDirective,
    LoadingComponent,
    TranslatePipe,
  ],
  templateUrl: './reservation.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class ReservationComponent implements OnInit {
  form!: FormGroup<{
    query: FormControl<string>;
    userId: FormControl<string>;
    reservationStatus: FormControl<ReservationStatus | null>;
    dateRange: FormControl<Date[] | null>;
    totalAmount: FormControl<string>;
    sortBy: FormControl<string>;
  }>;

  private fb = inject(NonNullableFormBuilder);
  private modalService = inject(NzModalService);
  readonly reservationStore = inject(ReservationStore);

  headerColumn = HEADER_COLUMNS.RESEVATION;
  reservationStatus = RESERVATION_STATUS;

  ngOnInit() {
    this.initialForm();
  }

  initialForm() {
    this.form = this.fb.group({
      query: [''],
      userId: [''],
      reservationStatus: this.fb.control<ReservationStatus | null>(null),
      dateRange: this.fb.control<Date[] | null>(null),
      totalAmount: [''],
      sortBy: ['newest'],
    });
  }

  private getFilter(pageIndex: number, pageSize: number) {
    const { query, userId, reservationStatus, dateRange, totalAmount, sortBy } =
      this.form.getRawValue();
    return {
      query: query.trim() || undefined,
      userId: userId ? Number(userId) : undefined,
      reservationStatus: reservationStatus || undefined,
      startDate: dateRange?.[0] ? formatDate(dateRange[0]) : undefined,
      endDate: dateRange?.[1] ? formatDate(dateRange[1]) : undefined,
      totalAmount: totalAmount ? Number(totalAmount) : undefined,
      sortBy: sortBy,
      page: pageIndex - 1,
      size: pageSize,
    };
  }

  onSubmit() {
    const currentSize = this.reservationStore.pagination()?.pageSize ?? 10;
    this.loadData(1, currentSize);
  }

  private loadData(page: number, size: number) {
    const filter = this.getFilter(page, size);
    this.reservationStore.loadReservations({ filter: filter });
  }

  openModal(reservationId: string) {
    this.modalService.create({
      nzContent: FormReservationComponent,
      nzTitle: undefined,
      nzClosable: true,
      nzData: {
        reservationId: reservationId,
      },
      nzWidth: 900,
      nzKeyboard: true,
      nzFooter: null,
    });
  }

  clearFilter() {
    this.form.reset({ sortBy: 'newest' });
    this.onSubmit();
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    const { pageIndex, pageSize } = params;
    this.loadData(pageIndex, pageSize);
  }
}
