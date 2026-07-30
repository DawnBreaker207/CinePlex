import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzModalModule, NzModalRef, NzModalService } from 'ng-zorro-antd/modal';
import { filter, map, Subject, take, takeUntil } from 'rxjs';
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
import { Actions, ofType } from '@ngrx/effects';
import { AsyncPipe } from '@angular/common';
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
import { Movie } from '@domain/movie/models/movie.model';
import { FormMovieAPIComponent } from '@features/admin/movie/form/api/api-form.component';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { TranslatePipe } from '@ngx-translate/core';

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
    AsyncPipe,
    NzAlertComponent,
    NzIconModule,
    NzSpinModule,
    TranslatePipe,
  ],
  templateUrl: './movie-form.component.html',
})
export class FormMovieComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private store = inject(Store);
  private actions$ = inject(Actions);
  private modelRef = inject(NzModalRef);
  private movieService = inject(MovieService);
  private modalService = inject(NzModalService);

  // Props
  mode: 'add' | 'edit' | 'view' = 'add';
  movieId!: number;
  form!: FormGroup;
  imageBase = environment.tmbd.imageUrl;

  // NgRx State
  loading$ = this.store.select(selectMovieLoadingDetails);
  saving$ = this.store.select(selectMoviesSaving);
  error$ = this.store.select(selectMoviesError);

  readonly submitting = signal(false);
  private initialFormValue: any = null;
  private destroy$: Subject<void> = new Subject<void>();

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
          takeUntil(this.destroy$),
        )
        .subscribe((movie) => {
          if (movie) {
            this.patchFormValue(movie);
            this.initialFormValue = this.form.value;
            if (this.mode === 'view') {
              this.disableForm();
            }
          } else {
            this.initialFormValue = this.form.value;
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
      duration: [null, [Validators.required, Validators.min(1)]],
      language: ['', Validators.required],
      country: ['', Validators.required],
      genres: [[], Validators.required],
      releaseDate: [null, Validators.required],
      imdbId: [''],
      filmId: [null],
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private disableForm() {
    Object.keys(this.form.controls).forEach((key) => {
      this.form.get(key)?.disable();
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
      Object.keys(this.form.controls).forEach((key) => {
        this.form.get(key)?.markAllAsTouched();
      });
      return;
    }

    this.submitting.set(true);
    const movieData = this.form.value;

    if (this.mode === 'edit') {
      this.store.dispatch(
        MovieActions.updateMovie({ id: this.movieId, movie: movieData }),
      );
    } else if (this.mode === 'add') {
      this.store.dispatch(MovieActions.createMovie({ movie: movieData }));
    }

    this.actions$
      .pipe(
        ofType(
          MovieActions.createMovieSuccess,
          MovieActions.updateMovieSuccess,
        ),
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe(() => {
        this.submitting.set(false);
        this.modelRef.close();
      });

    this.actions$
      .pipe(
        ofType(MovieActions.createMovieFailed, MovieActions.updateMovieFailed),
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe(({ error }) => {
        this.submitting.set(false);
        console.error('Save movie failed: ', error);
      });
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
          takeUntil(this.destroy$),
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
              filmId: result.id,
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
