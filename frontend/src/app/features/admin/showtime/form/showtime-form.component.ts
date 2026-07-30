import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { NzFormModule } from 'ng-zorro-antd/form';
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { Store } from '@ngrx/store';
import { Actions, ofType } from '@ngrx/effects';
import {
  debounceTime,
  distinctUntilChanged,
  filter,
  map,
  take,
} from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTimePickerModule } from 'ng-zorro-antd/time-picker';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { selectAllTheaters } from '@domain/theater/data-access/theater.selectors';
import { selectAllMovies } from '@domain/movie/data-access/movie.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { Movie } from '@domain/movie/models/movie.model';
import { Showtime } from '@domain/showtime/models/showtime.model';
import {
  formatDate,
  formatTime,
  timeFormatDay,
} from '@shared/utils/date.helper';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { TranslatePipe } from '@ngx-translate/core';

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
    SlicePipe,
    TranslatePipe,
  ],
  templateUrl: './showtime-form.component.html',
  styleUrl: './showtime-form.component.css',
})
export class FormShowtimeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private modelRef = inject(NzModalRef);
  private store = inject(Store);
  private actions$ = inject(Actions);

  mode: 'add' | 'edit' | 'view' = 'add';
  readonly submitting = signal(false);
  showtimeId!: number;
  form!: FormGroup;
  searchCtrl = this.fb.control('');

  theaters = this.store.selectSignal(selectAllTheaters);
  searchMovie = this.store.selectSignal(selectAllMovies);
  isSearching = signal(false);
  selectedMovie = signal<any>(null);
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
          takeUntilDestroyed(),
        )
        .subscribe((theater) => {
          if (theater) {
            this.patchFormValue(theater);
            if (this.mode === 'view') {
              this.disableForm();
            }
          }
        });
    }
    this.search();
  }

  search() {
    this.searchCtrl.valueChanges
      .pipe(
        debounceTime(300),
        map((v) => v?.trim()),
        distinctUntilChanged(),
      )
      .subscribe((search) => {
        if (search) {
          this.isSearching.set(true);
          this.store.dispatch(MovieActions.searchMovies({ search: search }));
        } else {
          this.isSearching.set(false);
        }
      });
  }

  private disableForm() {
    Object.keys(this.form.controls).forEach((key) => {
      this.form.get(key)?.disable();
    });
  }

  selectMovie(movie: Movie) {
    const movieResult = this.searchMovie().find((m) => m.id === movie?.id);
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
      movieId: ['', Validators.required],
      moviePosterUrl: [''],
      theaterId: ['', Validators.required],

      showDate: [null, Validators.required],
      showTime: [null, Validators.required],

      price: [null, [Validators.required, Validators.min(1)]],
      totalSeats: [null, [Validators.required, Validators.min(1)]],
    });
  }

  private patchFormValue(showtime: Showtime) {
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
      showTime: timeFormatDay(showtime.showTime),
    });
  }

  submit() {
    if (this.form.invalid || this.submitting()) {
      Object.keys(this.form.controls).forEach((key) => {
        this.form.get(key)?.markAllAsTouched();
      });
      return;
    }

    this.submitting.set(true);
    const showtimeData = {
      ...this.form.value,
      showDate: this.form.value.showDate
        ? formatDate(this.form.value.showDate)
        : null,
      showTime: this.form.value.showTime
        ? formatTime(this.form.value.showDate)
        : null,
    };

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

    this.actions$
      .pipe(
        ofType(
          ShowtimeActions.createShowtimeSuccess,
          ShowtimeActions.updateShowtimeSuccess,
        ),
        take(1),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.submitting.set(false);
        this.modelRef.close();
      });

    this.actions$
      .pipe(
        ofType(
          ShowtimeActions.createShowtimeFailed,
          ShowtimeActions.updateShowtimeFailed,
        ),
        take(1),
        takeUntilDestroyed(),
      )
      .subscribe(({ error }) => {
        this.submitting.set(false);
        console.error('Save showtime failed', error);
      });
  }

}
