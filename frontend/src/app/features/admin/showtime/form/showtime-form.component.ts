import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ChangeDetectionStrategy } from '@angular/core';
import { NzFormModule } from 'ng-zorro-antd/form';
import {
  FormControl,
  FormGroup,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { Store } from '@ngrx/store';
import { Actions } from '@ngrx/effects';
import { filter, take } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTimePickerModule } from 'ng-zorro-antd/time-picker';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { selectAllTheaters } from '@domain/theater/data-access/theater.selectors';
import { selectAllMovies } from '@domain/movie/data-access/movie.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { Movie } from '@domain/movie/models/movie.model';
import {
  Showtime,
  ShowtimeRequest,
} from '@domain/showtime/models/showtime.model';
import {
  formatDate,
  formatTime,
  timeFormatDay,
} from '@shared/utils/date.helper';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { RoomService } from '@domain/theater/data-access/room.service';
import { Room } from '@domain/theater/models/room.model';
import { TranslatePipe } from '@ngx-translate/core';
import {
  debouncedSearch,
  disableForm,
  markFormTouched,
  watchSaveResult,
} from '@shared/utils/form-utils';

@Component({
  selector: 'app-form',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    FormsModule,
    NzDatePickerModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    NzTimePickerModule,
    NzIconModule,
    NzInputNumberModule,
    NzTagModule,
    NzButtonModule,
    SlicePipe,
    TranslatePipe,
  ],
  templateUrl: './showtime-form.component.html',
  styleUrl: './showtime-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormShowtimeComponent implements OnInit {
  private fb = inject(NonNullableFormBuilder);
  private modelRef = inject(NzModalRef);
  private roomService = inject(RoomService);
  private store = inject(Store);
  private actions$ = inject(Actions);
  private destroyRef = inject(DestroyRef);

  mode: 'add' | 'edit' | 'view' = 'add';
  readonly submitting = signal(false);
  showtimeId!: number;
  form!: FormGroup<{
    movieId: FormControl<number | null>;
    moviePosterUrl: FormControl<string>;
    theaterId: FormControl<number | null>;
    roomId: FormControl<number | null>;
    showDate: FormControl<Date | null>;
    showTime: FormControl<Date | null>;
    price: FormControl<number | null>;
    totalSeats: FormControl<number | null>;
  }>;
  searchCtrl = this.fb.control('');
  readonly rooms = signal<Room[]>([]);

  theaters = this.store.selectSignal(selectAllTheaters);
  searchMovie = this.store.selectSignal(selectAllMovies);
  isSearching = signal(false);
  selectedMovie = signal<Movie | null>(null);
  searchResults = computed(() => {
    return this.isSearching() && this.searchCtrl.value
      ? this.searchMovie()
      : [];
  });

  ngOnInit() {
    this.initForm();
    this.store.dispatch(TheaterActions.loadTheaters({ page: 0, size: 10 }));
    const { mode, id } = this.modelRef.getConfig().nzData;
    this.mode = mode;
    this.showtimeId = id;
    if (this.mode !== 'add' && this.showtimeId) {
      this.store.dispatch(
        ShowtimeActions.loadShowtime({ id: this.showtimeId as number }),
      );
      this.store
        .select(selectSelectedShowtime)
        .pipe(
          filter(
            (showtime) => showtime !== null && showtime.id === this.showtimeId,
          ),
          take(1),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((theater) => {
          if (theater) {
            this.patchFormValue(theater);
            if (this.mode === 'view') {
              disableForm(this.form);
            }
          }
        });
    }
    this.search();
  }

  search() {
    debouncedSearch(this.searchCtrl, 300)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((search) => {
        if (search) {
          this.isSearching.set(true);
          this.store.dispatch(MovieActions.searchMovies({ search: search }));
        } else {
          this.isSearching.set(false);
        }
      });
  }

  clearSelectedMovie() {
    this.selectedMovie.set(null);
    this.form.controls.movieId.setValue(null);
  }

  selectMovie(movie: Movie) {
    const movieResult =
      this.searchMovie().find((m) => m.id === movie?.id) ?? null;
    this.form.patchValue({
      movieId: movie.id,
      moviePosterUrl: movie.poster,
    });
    this.selectedMovie.set(movieResult);
    this.isSearching.set(false);
    this.searchCtrl.setValue('', { emitEvent: false });
  }
  initForm() {
    this.form = this.fb.group({
      movieId: [null as number | null, Validators.required],
      moviePosterUrl: [''],
      theaterId: [null as number | null, Validators.required],
      roomId: [null as number | null],

      showDate: [null as Date | null, Validators.required],
      showTime: [null as Date | null, Validators.required],

      price: [null as number | null, [Validators.required, Validators.min(1)]],
      totalSeats: [
        null as number | null,
        [Validators.required, Validators.min(1)],
      ],
    });
    this.form.controls.theaterId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((theaterId) => this.loadRooms(theaterId));
  }

  loadRooms(theaterId: number | null) {
    this.rooms.set([]);
    this.form.controls.roomId.setValue(null, { emitEvent: false });
    if (!theaterId) return;
    this.roomService.getByTheater(theaterId).subscribe({
      next: (rooms) => this.rooms.set(rooms),
      error: () => this.rooms.set([]),
    });
  }

  onRoomChange(roomId: number | null) {
    const room = this.rooms().find((r) => r.id === roomId);
    if (room?.totalSeats) {
      this.form.controls.totalSeats.setValue(room.totalSeats);
    }
  }

  private patchFormValue(showtime: Showtime) {
    this.loadRooms(showtime.theaterId);
    this.form.patchValue({
      // Theater
      theaterId: showtime.theaterId,
      // Movie
      movieId: showtime.movieId,
      moviePosterUrl: showtime.moviePosterUrl,
      // Showtime
      price: showtime.price,
      totalSeats: showtime.totalSeats,
      // Showtime Date
      showDate: showtime.showDate,
      showTime: timeFormatDay(showtime.showTime) as unknown as Date,
    });
  }

  submit() {
    if (this.form.invalid || this.submitting()) {
      markFormTouched(this.form);
      return;
    }

    this.submitting.set(true);
    const { showDate, showTime, roomId: _roomId, ...rest } = this.form.getRawValue();
    const showtimeData = {
      ...rest,
      showDate: showDate ? formatDate(showDate) : null,
      showTime: showTime ? formatTime(showTime) : null,
    } as unknown as ShowtimeRequest;

    if (this.mode === 'edit') {
      this.store.dispatch(
        ShowtimeActions.updateShowtime({
          id: this.showtimeId as number,
          showtime: showtimeData,
        }),
      );
    } else {
      this.store.dispatch(
        ShowtimeActions.createShowtime({ showtime: showtimeData }),
      );
    }

    watchSaveResult(
      this.actions$,
      this.destroyRef,
      [
        ShowtimeActions.createShowtimeSuccess,
        ShowtimeActions.updateShowtimeSuccess,
      ],
      [
        ShowtimeActions.createShowtimeFailed,
        ShowtimeActions.updateShowtimeFailed,
      ],
      () => {
        this.submitting.set(false);
        this.modelRef.close();
      },
      (error) => {
        this.submitting.set(false);
        console.error('Save showtime failed', error);
      },
    );
  }
}
