import { Component, inject, OnInit } from '@angular/core';
import { NzModalService } from 'ng-zorro-antd/modal';
import { Store } from '@ngrx/store';
import { DatePipe } from '@angular/common';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import {
  NzTableModule,
  NzTableQueryParams,
  NzTheadComponent,
  NzThMeasureDirective,
} from 'ng-zorro-antd/table';
import { NzWaveModule } from 'ng-zorro-antd/core/wave';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { filter, take } from 'rxjs';
import {
  FormControl,
  FormGroup,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
} from '@angular/forms';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { ICON_COLORS, CHART_COLORS } from '@shared/utils/colors';
import {
  selectAllShowtimes,
  selectPaginationShowtime,
  selectShowtimeError,
  selectShowtimeLoading,
} from '@domain/showtime/data-access/showtime.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { selectAllTheaters } from '@domain/theater/data-access/theater.selectors';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { formatDate } from '@shared/utils/date.helper';
import { FormShowtimeComponent } from '@features/admin/showtime/form/showtime-form.component';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { HEADER_COLUMNS } from '@core/constants/column';

@Component({
  selector: 'app-showtime',
  imports: [
    DatePipe,
    NzButtonModule,
    NzIconModule,
    NzSelectModule,
    NzSpaceModule,
    NzThMeasureDirective,
    NzTheadComponent,
    NzTableModule,
    NzWaveModule,
    NzSpinModule,
    NzAlertModule,
    ReactiveFormsModule,
    NzFormModule,
    FormsModule,
    NzDatePickerModule,
    NzTagModule,
    NzTooltipModule,
    NzProgressModule,
    NzDropDownModule,
    LoadingComponent,
    TranslatePipe,
  ],
  templateUrl: './showtime.component.html',
  providers: [NzModalService],
})
export class ShowtimeComponent implements OnInit {
  private modalService = inject(NzModalService);
  private store = inject(Store);
  private fb = inject(NonNullableFormBuilder);

  showtimes = this.store.selectSignal(selectAllShowtimes);
  theaters = this.store.selectSignal(selectAllTheaters);
  pagination = this.store.selectSignal(selectPaginationShowtime);
  loading = this.store.selectSignal(selectShowtimeLoading);
  error = this.store.selectSignal(selectShowtimeError);

  headerColumn = HEADER_COLUMNS.SHOWTIME;
  readonly ICON_COLORS = ICON_COLORS;
  readonly progressStrokeColor = { '0%': CHART_COLORS.success(), '100%': CHART_COLORS.primary() };
  form!: FormGroup<{
    theaterId: FormControl<number | null>;
    dateRange: FormControl<Date[] | null>;
  }>;

  constructor() {
    this.initForm();
    this.form.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.loadData(1));
  }

  ngOnInit() {
    this.store.dispatch(TheaterActions.loadTheaters({ page: 0, size: 10 }));
    this.store
      .select(selectAllTheaters)
      .pipe(
        filter((t) => t.length > 0),
        take(1),
      )
      .subscribe((theaters) => {
        if (!this.form.get('theaterId')?.value) {
          this.form.patchValue({ theaterId: theaters[0].id });
        }
      });
  }

  loadData(pageIndex: number, pageSize?: number) {
    const { theaterId, dateRange } = this.form.getRawValue();
    if (!theaterId) return;
    const size = pageSize ?? this.pagination()?.pageSize ?? 10;
    this.store.dispatch(
      ShowtimeActions.loadShowtimesByTheaterId({
        theaterId: theaterId,
        dateRange: {
          startDate: dateRange ? formatDate(dateRange[0]) : undefined,
          endDate: dateRange ? formatDate(dateRange[1]) : undefined,
        },
        page: pageIndex - 1,
        size: size,
      }),
    );
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    const { pageIndex, pageSize } = params;
    this.loadData(pageIndex, pageSize);
  }

  openModal(mode: 'add' | 'edit' | 'view', id?: number) {
    const modal = this.modalService.create({
      nzContent: FormShowtimeComponent,
      nzTitle: undefined,
      nzClosable: true,
      nzData: {
        mode: mode,
        id: id,
      },
      nzWidth: 900,
      nzKeyboard: true,
      nzFooter:
        mode === 'view'
          ? null
          : [
              {
                label: 'Lưu',
                type: 'primary',
                onClick: () => {
                  const form =
                    modal.getContentComponent() as FormShowtimeComponent;
                  if (form.form.valid && !form.submitting()) {
                    form.submit();
                  }
                },
              },
            ],
    });
  }

  onDelete(id: number) {
    this.modalService.confirm({
      nzTitle: 'Xóa suất chiếu này?',
      nzContent: 'Dữ liệu sẽ không thể khôi phục sau khi xóa.',
      nzOkText: 'Xóa',
      nzOkDanger: true,
      nzOnOk: () => this.store.dispatch(ShowtimeActions.deleteShowtime({ id })),
      nzCancelText: 'Hủy',
    });
  }

  resetForm() {
    this.form.reset();
    const theaters = this.theaters();
    if (theaters.length > 0) {
      this.form.patchValue({ theaterId: theaters[0].id });
    }
  }

  private initForm() {
    this.form = this.fb.group({
      theaterId: this.fb.control<number | null>(null),
      dateRange: this.fb.control<Date[] | null>(null),
    });
  }
}
