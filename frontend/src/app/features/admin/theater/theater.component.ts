import { Component, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NzModalService } from 'ng-zorro-antd/modal';
import { DatePipe } from '@angular/common';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpaceComponent } from 'ng-zorro-antd/space';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';
import { Store } from '@ngrx/store';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import {
  selectAllTheaters,
  selectPaginationTheater,
  selectTheaterError,
  selectTheaterLoading,
} from '@domain/theater/data-access/theater.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { FormTheaterComponent } from '@features/admin/theater/form/theater-form.component';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { TranslatePipe } from '@ngx-translate/core';
import { HEADER_COLUMNS } from '@core/constants/column';
import { ICON_COLORS } from '@shared/utils/colors';

@Component({
  selector: 'app-theater',
  imports: [
    NzTableModule,
    NzInputModule,
    NzSelectModule,
    NzButtonModule,
    NzIconModule,
    DatePipe,
    NzSpaceComponent,
    ReactiveFormsModule,
    NzAlertComponent,
    LoadingComponent,
    TranslatePipe,
  ],
  providers: [NzModalService],
  templateUrl: './theater.component.html',
})
export class TheaterComponent {
  private modalService = inject(NzModalService);
  private store = inject(Store);
  private fb = inject(FormBuilder);

  readonly headerColumn = HEADER_COLUMNS.THEATER;
  readonly ICON_COLORS = ICON_COLORS;
  theaters = this.store.selectSignal(selectAllTheaters);
  pagination = this.store.selectSignal(selectPaginationTheater);
  loading = this.store.selectSignal(selectTheaterLoading);
  error = this.store.selectSignal(selectTheaterError);

  searchCtrl = this.fb.control('');
  sortCtrl = this.fb.control('newest');

  filteredTheaters = computed(() => {
    const list = this.theaters();
    const q = (this.searchCtrl.value || '').toLowerCase().trim();
    const sort = this.sortCtrl.value || 'newest';
    let result = q
      ? list.filter(t => t.name.toLowerCase().includes(q) || t.location.toLowerCase().includes(q))
      : list;
    if (sort === 'oldest') result = [...result].reverse();
    return result;
  });

  openModal(mode: 'add' | 'edit' | 'view', id?: number) {
    const modal = this.modalService.create({
      nzContent: FormTheaterComponent,
      nzTitle: undefined,
      nzClosable: true,
      nzData: {
        mode: mode,
        id: id,
      },
      nzWidth: 900,
      nzKeyboard: true,
      nzFooter:
        mode !== 'view'
          ? [
              {
                label: 'Lưu',
                type: 'primary',
                onClick: () => {
                  const form =
                    modal.getContentComponent() as FormTheaterComponent;
                  if (form.form.valid && !form.submitting()) {
                    form.submit();
                  }
                },
              },
            ]
          : null,
    });
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    const { pageIndex, pageSize } = params;
    this.store.dispatch(
      TheaterActions.loadTheaters({ page: pageIndex - 1, size: pageSize }),
    );
  }

  onDelete(id: number) {
    this.modalService.confirm({
      nzTitle: 'Xóa rạp chiếu này?',
      nzContent: 'Dữ liệu sẽ không thể khôi phục sau khi xóa.',
      nzOkText: 'Xóa',
      nzOkDanger: true,
      nzOnOk: () => this.store.dispatch(TheaterActions.deleteTheater({ id })),
      nzCancelText: 'Hủy',
    });
  }
}
