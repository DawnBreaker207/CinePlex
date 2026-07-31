import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NonNullableFormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { NzModalModule, NzModalRef, NzModalService } from 'ng-zorro-antd/modal';
import { filter, map, take } from 'rxjs';
import { NzAutosizeDirective, NzInputDirective } from 'ng-zorro-antd/input';
import {
  NzFormControlComponent,
  NzFormDirective,
  NzFormLabelComponent,
} from 'ng-zorro-antd/form';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzOptionComponent, NzSelectComponent } from 'ng-zorro-antd/select';
import { NzDatePickerComponent } from 'ng-zorro-antd/date-picker';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { Store } from '@ngrx/store';
import { Actions } from '@ngrx/effects';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { MovieService } from '@domain/movie/data-access/movie.service';
import { environment } from '@env/environment';
import {
  selectMovieById,
  selectMovieLoadingDetails,
  selectMoviesError,
  selectMoviesSaving,
} from '@domain/movie/data-access/movie.selectors';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { Movie, MovieRequest } from '@domain/movie/models/movie.model';
import { FormMovieAPIComponent } from '@features/admin/movie/form/api/api-form.component';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { TranslatePipe } from '@ngx-translate/core';
import {
  disableForm,
  markFormTouched,
  watchSaveResult,
} from '@shared/utils/form-utils';

@Component({
  selector: 'app-movie-form',
  imports: [
    NzModalModule,
    NzInputDirective,
    ReactiveFormsModule,
    NzFormDirective,
    NzGridModule,
    NzFormControlComponent,
    NzSelectComponent,
    NzOptionComponent,
    NzDatePickerComponent,
    NzFormLabelComponent,
    NzButtonComponent,
    NzSpaceModule,
    NzAutosizeDirective,
    NzAlertComponent,
    NzIconModule,
    NzSpinModule,
    TranslatePipe,
  ],
  templateUrl: './movie-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormMovieComponent implements OnInit {
  private fb = inject(NonNullableFormBuilder);
  private store = inject(Store);
  private actions$ = inject(Actions);
  private modelRef = inject(NzModalRef);
  private movieService = inject(MovieService);
  private modalService = inject(NzModalService);
  private destroyRef = inject(DestroyRef);

  // Props
  mode: 'add' | 'edit' | 'view' = 'add';
  movieId!: number;
  form!: FormGroup<{
    id: FormControl<string>;
    title: FormControl<string>;
    originalTitle: FormControl<string>;
    poster: FormControl<string>;
    backdrop: FormControl<string>;
    overview: FormControl<string>;
    duration: FormControl<number | null>;
    language: FormControl<string>;
    country: FormControl<string>;
    genres: FormControl<string[]>;
    releaseDate: FormControl<Date | null>;
    imdbId: FormControl<string>;
    filmId: FormControl<string | null>;
  }>;
  imageBase = environment.tmbd.imageUrl;

  // NgRx State
  private loading$ = this.store.select(selectMovieLoadingDetails);
  private saving$ = this.store.select(selectMoviesSaving);
  private error$ = this.store.select(selectMoviesError);
  readonly loading = toSignal(this.loading$, { initialValue: false });
  readonly saving = toSignal(this.saving$, { initialValue: false });
  readonly error = toSignal(this.error$, { initialValue: null });

  readonly submitting = signal(false);
  private initialFormValue: MovieRequest | null = null;

  ngOnInit() {
    this.initForm();
    const { mode, movieId } = this.modelRef.getConfig().nzData;
    this.mode = mode;
    this.movieId = movieId;

    if (this.mode !== 'add' && this.movieId) {
      this.store.dispatch(MovieActions.loadMovie({ id: this.movieId }));
      this.store
        .select(selectMovieById(this.movieId))
        .pipe(
          filter((movie) => movie !== null && movie.id === this.movieId),
          take(1),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((movie) => {
          if (movie) {
            this.patchFormValue(movie);
            this.initialFormValue = this.form.getRawValue() as MovieRequest;
            if (this.mode === 'view') {
              disableForm(this.form);
            }
          } else {
            this.initialFormValue = this.form.getRawValue() as MovieRequest;
          }
        });
    }
  }

  hasData(): boolean {
    if (this.mode === 'view') return false;

    if (!this.initialFormValue) return false;

    return (
      JSON.stringify(this.initialFormValue) !== JSON.stringify(this.form.value)
    );
  }

  initForm() {
    this.form = this.fb.group({
      id: [''],
      title: ['', Validators.required],
      originalTitle: ['', Validators.required],
      poster: ['', Validators.required],
      backdrop: [''],
      overview: ['', Validators.required],
      duration: [
        null as number | null,
        [Validators.required, Validators.min(1)],
      ],
      language: ['', Validators.required],
      country: ['', Validators.required],
      genres: [[] as string[], Validators.required],
      releaseDate: [null as Date | null, Validators.required],
      imdbId: [''],
      filmId: [null as string | null],
    });
  }

  private patchFormValue(movie: Movie) {
    this.form.patchValue({
      title: movie.title,
      originalTitle: movie.originalTitle,
      poster: movie.poster,
      backdrop: movie.backdrop,
      overview: movie.overview,
      duration: movie.duration,
      language: movie.language,
      country: movie.country,
      genres: movie.genres,
      releaseDate: movie.releaseDate,
      imdbId: movie.imdbId,
      filmId: movie.filmId,
    });
  }

  submit() {
    if (!this.form.valid || this.submitting()) {
      markFormTouched(this.form);
      return;
    }

    this.submitting.set(true);
    const movieData = this.form.getRawValue() as MovieRequest;

    if (this.mode === 'edit') {
      this.store.dispatch(
        MovieActions.updateMovie({ id: this.movieId, movie: movieData }),
      );
    } else if (this.mode === 'add') {
      this.store.dispatch(MovieActions.createMovie({ movie: movieData }));
    }

    watchSaveResult(
      this.actions$,
      this.destroyRef,
      [MovieActions.createMovieSuccess, MovieActions.updateMovieSuccess],
      [MovieActions.createMovieFailed, MovieActions.updateMovieFailed],
      () => {
        this.submitting.set(false);
        this.modelRef.close();
      },
      (error) => {
        this.submitting.set(false);
        console.error('Save movie failed: ', error);
      },
    );
  }

  openMovieModal() {
    const modal = this.modalService.create({
      nzContent: FormMovieAPIComponent,
      nzTitle: 'Search Movie',
      nzClosable: true,
      nzWidth: 800,
      nzKeyboard: true,
      nzFooter: null,
    });

    modal.afterClose.subscribe((selectMovie: any) => {
      if (!selectMovie) return;

      this.movieService
        .getMovieDetails(selectMovie.id)
        .pipe(
          map((movie: any) => ({
            ...movie,
            poster_path: movie.poster_path
              ? `${this.imageBase}/${movie.poster_path}`
              : '',
            backdrop_path: movie.backdrop_path
              ? `${this.imageBase}/${movie.backdrop_path}`
              : '',
            genres: movie.genres
              ? movie.genres.map((g: any) => g.name.replace(/^Phim\s/i, ''))
              : [],
          })),
          take(1),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (result: any) => {
            this.form.patchValue({
              title: result.title,
              originalTitle: result.original_title,
              poster: result.poster_path,
              backdrop: result.backdrop_path,
              overview: result.overview,
              duration: result.runtime,
              language: result.original_language,
              country: result.origin_country[0],
              genres: result.genres,
              releaseDate: result.release_date,
              imdbId: result.imdb_id,
              filmId: String(result.id),
            });
            this.form.markAsDirty();
          },
          error: (error) => {
            console.error('Failed to fetch movie details', error);
          },
        });
    });
  }
}
