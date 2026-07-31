import {
  Component,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { filter, take } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NzInputDirective } from 'ng-zorro-antd/input';
import { NzColDirective, NzRowDirective } from 'ng-zorro-antd/grid';
import {
  NzFormControlComponent,
  NzFormDirective,
  NzFormLabelComponent,
} from 'ng-zorro-antd/form';
import { Store } from '@ngrx/store';
import { Actions } from '@ngrx/effects';
import { NzOptionComponent, NzSelectComponent } from 'ng-zorro-antd/select';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { selectTheaterById } from '@domain/theater/data-access/theater.selectors';
import { Theater, TheaterRequest } from '@domain/theater/models/theater.model';
import { TranslatePipe } from '@ngx-translate/core';
import {
  disableForm,
  markFormTouched,
  watchSaveResult,
} from '@shared/utils/form-utils';

@Component({
  selector: 'app-theater-form',
  imports: [
    FormsModule,
    NzColDirective,
    NzFormControlComponent,
    NzFormDirective,
    NzFormLabelComponent,
    NzInputDirective,
    NzRowDirective,
    ReactiveFormsModule,
    NzOptionComponent,
    NzSelectComponent,
    TranslatePipe,
  ],
  templateUrl: './theater-form.component.html',
  styleUrl: './theater-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormTheaterComponent implements OnInit {
  readonly submitting = signal(false);
  mode: 'add' | 'edit' | 'view' = 'add';
  theaterId!: number;
  form!: FormGroup<{
    id: FormControl<string>;
    name: FormControl<string>;
    location: FormControl<string>;
    capacity: FormControl<number | null>;
  }>;
  private fb = inject(NonNullableFormBuilder);
  private modelRef = inject(NzModalRef);
  private store = inject(Store);
  private actions$ = inject(Actions);

  private initialForm: any = null;

  ngOnInit() {
    this.initForm();

    const { mode, id } = this.modelRef.getConfig().nzData;

    this.mode = mode;
    this.theaterId = id;
    if (this.mode !== 'add' && this.theaterId) {
      this.store.dispatch(
        TheaterActions.loadTheater({ id: this.theaterId as number }),
      );
      this.store
        .select(selectTheaterById(this.theaterId))
        .pipe(
          filter(
            (theater) => theater !== null && theater.id === this.theaterId,
          ),
          take(1),
          takeUntilDestroyed(),
        )
        .subscribe((theater) => {
          if (theater) {
            this.patchFormValue(theater);
            this.initialForm = this.form.value;
            if (this.mode === 'view') {
              disableForm(this.form);
            }
          } else {
            this.initialForm = this.form.value;
          }
        });
    }
  }

  initForm() {
    this.form = this.fb.group({
      id: [''],
      name: ['', Validators.required],
      location: ['', Validators.required],
      capacity: [
        null as number | null,
        [Validators.required, Validators.min(1)],
      ],
    });
  }

  private patchFormValue(theater: Theater) {
    this.form.patchValue({
      name: theater.name,
      location: theater.name,
      capacity: theater.capacity,
    });
  }

  hasData(): boolean {
    return Object.values(this.form.value).some(
      (v) => v !== null && v !== '' && !(Array.isArray(v) && v.length === 0),
    );
  }

  submit() {
    if (!this.form.valid || this.submitting()) {
      markFormTouched(this.form);
      return;
    }

    this.submitting.set(true);
    const theaterData = this.form.getRawValue() as TheaterRequest;
    if (this.mode === 'edit') {
      this.store.dispatch(
        TheaterActions.updateTheater({
          id: this.theaterId as number,
          theater: theaterData as Theater,
        }),
      );
    } else if (this.mode === 'add') {
      this.store.dispatch(
        TheaterActions.createTheater({ theater: theaterData }),
      );
    }

    watchSaveResult(
      this.actions$,
      [
        TheaterActions.createTheaterSuccess,
        TheaterActions.updateTheaterSuccess,
      ],
      [TheaterActions.createTheaterFailed, TheaterActions.updateTheaterFailed],
      () => {
        this.submitting.set(false);
        this.modelRef.close();
      },
      (error) => {
        this.submitting.set(false);
        console.error('Save theater failed', error);
      },
    );
  }
}
