import {
  Component,
  computed,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NzModalService } from 'ng-zorro-antd/modal';
import { DatePipe } from '@angular/common';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpaceComponent } from 'ng-zorro-antd/space';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
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
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { HEADER_COLUMNS } from '@core/constants/column';
import { ICON_COLORS } from '@shared/utils/colors';
import { confirmDelete } from '@shared/utils/confirm-delete';
import { saveFooter } from '@shared/utils/form-utils';

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
NzToolTipModule,
ReactiveFormsModule,
    NzAlertComponent,
    LoadingComponent,
    TranslatePipe,
  ],
  providers: [NzModalService],
  templateUrl: './theater.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TheaterComponent {
  private modalService = inject(NzModalService);
  private router = inject(Router);
private store = inject(Store);
private translate = inject(TranslateService);
  private fb = inject(NonNullableFormBuilder);

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
      ? list.filter(
          (t) =>
            t.name.toLowerCase().includes(q) ||
            t.location.toLowerCase().includes(q),
        )
      : list;
    if (sort === 'oldest') result = [...result].reverse();
    return result;
  });

  openModal(mode: 'add' | 'edit' | 'view', id?: number) {
    this.modalService.create({
      nzContent: FormTheaterComponent,
      nzTitle: undefined,
      nzClosable: true,
      nzData: {
        mode: mode,
        id: id,
      },
      nzWidth: 900,
      nzKeyboard: true,
      nzFooter: mode !== 'view' ? saveFooter<FormTheaterComponent>(this.translate) : null,
    });
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    const { pageIndex, pageSize } = params;
    this.store.dispatch(
      TheaterActions.loadTheaters({ page: pageIndex - 1, size: pageSize }),
    );
  }

  goRooms(id: number) {
    this.router.navigate(['/admin/theater', id, 'rooms']);
  }

  onDelete(id: number) {
    confirmDelete(this.modalService, this.translate, 'theater.deleteTitle', () =>
      this.store.dispatch(TheaterActions.deleteTheater({ id })),
    );
  }
}
