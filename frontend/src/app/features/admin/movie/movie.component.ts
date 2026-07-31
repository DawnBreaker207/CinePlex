import { Component, computed, inject, signal } from '@angular/core';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzTagComponent } from 'ng-zorro-antd/tag';
import { DatePipe, NgClass } from '@angular/common';
import { NzSpaceComponent } from 'ng-zorro-antd/space';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NzAvatarComponent } from 'ng-zorro-antd/avatar';
import {
  NzListComponent,
  NzListItemComponent,
  NzListItemMetaComponent,
} from 'ng-zorro-antd/list';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';
import { Store } from '@ngrx/store';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { IsDeletedPipe } from '@shared/pipes/is-deleted.pipe';
import {
  selectAllMovies,
  selectMovieLoading,
  selectMoviesError,
  selectPaginationMovie,
} from '@domain/movie/data-access/movie.selectors';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { FormMovieComponent } from '@features/admin/movie/form/movie/movie-form.component';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { HEADER_COLUMNS } from '@core/constants/column';
import { ICON_COLORS } from '@shared/utils/colors';

@Component({
  selector: 'app-movie',
  imports: [
    NzTableModule,
    NzInputModule,
    NzSelectModule,
    NzButtonModule,
    NzIconModule,
    NzTagComponent,
    DatePipe,
    NzSpaceComponent,
    NzAvatarComponent,
    NzListComponent,
    NzListItemComponent,
    NzListItemMetaComponent,
    NgClass,
    ReactiveFormsModule,
    NzAlertComponent,
    NzSpinModule,
    IsDeletedPipe,
    LoadingComponent,
    TranslatePipe,
  ],
  templateUrl: './movie.component.html',
  providers: [NzModalService],
})
export class MovieComponent {
  private fb = inject(NonNullableFormBuilder);
  private store = inject(Store);
  private modalService = inject(NzModalService);

  // NgRx Selectors
  readonly movies = this.store.selectSignal(selectAllMovies);
  readonly pagination = this.store.selectSignal(selectPaginationMovie);
  readonly loading = this.store.selectSignal(selectMovieLoading);
  readonly error = this.store.selectSignal(selectMoviesError);

  // Local State
  readonly headerColumn = HEADER_COLUMNS.MOVIE;
  readonly ICON_COLORS = ICON_COLORS;
  readonly searchCtrl = this.fb.control('');

  isSearching = signal(false);
  selectedMovie = signal<any>(null);
  searchResults = computed(() => {
    if (!this.isSearching()) return [];
    const q = (this.searchCtrl.value || '').toLowerCase().trim();
    if (!q) return [];
    return this.movies().filter(m =>
      m.title.toLowerCase().includes(q) ||
      (m.originalTitle || '').toLowerCase().includes(q),
    );
  });

  constructor() {
    this.searchCtrl.valueChanges
      .pipe(
        debounceTime(500),
        map((v) => v?.trim() || ''),
        distinctUntilChanged(),
        takeUntilDestroyed(),
      )
      .subscribe((search) => {
        if (search) {
          this.isSearching.set(true);
          this.store.dispatch(MovieActions.searchMovies({ search: search }));
        } else {
          this.isSearching.set(false);
          this.store.dispatch(MovieActions.loadMovies({ page: 0, size: 10 }));
        }
      });
  }

  selectMovie(id: number) {
    const movie = this.movies().find((m) => m.id === id);
    if (movie) {
      this.store.dispatch(MovieActions.selectedMovie({ movie }));
      this.selectedMovie.set(movie);
      this.openMovieModal('view', id);
    }
    this.isSearching.set(false);
    this.searchCtrl.setValue('', { emitEvent: false });
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    const { pageIndex, pageSize } = params;
    this.store.dispatch(
      MovieActions.loadMovies({ page: pageIndex - 1, size: pageSize }),
    );
  }

  openMovieModal(mode: 'add' | 'edit' | 'view', id?: string | number) {
    const modal = this.modalService.create({
      nzContent: FormMovieComponent,
      nzTitle: mode,
      nzClosable: true,
      nzData: {
        mode: mode,
        movieId: id,
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
                    modal.getContentComponent() as FormMovieComponent;
                  if (form.form.valid && !form.submitting()) {
                    form.submit();
                  }
                },
              },
            ],
      nzOnCancel: (instance) => this.handleCancel(instance),
    });
  }

  private handleCancel(instance: FormMovieComponent) {
    if (!instance || !instance.hasData()) return true;

    return new Promise((resolve) => {
      this.modalService.confirm({
        nzTitle: 'Bạn có thay đổi chưa lưu. Hủy thao tác?',
        nzContent: 'Thông tin bạn đã nhập sẽ bị mất.',
        nzOkText: 'Hủy thao tác',
        nzOkDanger: true,
        nzOnOk: () => resolve(true),
        nzCancelText: 'Tiếp tục chỉnh sửa',
        nzOnCancel: () => resolve(false),
      });
    });
  }

  onDelete(id: number) {
    this.modalService.confirm({
      nzTitle: 'Xóa phim này?',
      nzContent: 'Dữ liệu sẽ không thể khôi phục sau khi xóa.',
      nzOkText: 'Xóa',
      nzOkDanger: true,
      nzOnOk: () => this.store.dispatch(MovieActions.deleteMovie({ id })),
      nzCancelText: 'Hủy',
    });
  }
}
