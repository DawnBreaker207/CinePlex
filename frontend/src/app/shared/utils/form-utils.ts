import { AbstractControl, FormGroup } from '@angular/forms';
import { DestroyRef } from '@angular/core';
import { Actions, ofType } from '@ngrx/effects';
import { ActionCreator } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { ModalButtonOptions } from 'ng-zorro-antd/modal';
import { debounceTime, distinctUntilChanged, map, Observable } from 'rxjs';
import { take } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export const markFormTouched = (form: FormGroup): void => {
  Object.keys(form.controls).forEach((key) =>
    form.get(key)?.markAllAsTouched(),
  );
};

export const disableForm = (form: FormGroup): void => {
  Object.keys(form.controls).forEach((key) => form.get(key)?.disable());
};

export const debouncedSearch = (
  control: AbstractControl,
  debounceMs: number,
): Observable<string> =>
  control.valueChanges.pipe(
    debounceTime(debounceMs),
    map((v) => (typeof v === 'string' ? v.trim() : '')),
    distinctUntilChanged(),
  );

export function watchSaveResult(
  actions$: Actions,
  destroyRef: DestroyRef,
  successTypes: ActionCreator[],
  failTypes: ActionCreator[],
  onSuccess: () => void,
  onError: (error: unknown) => void,
): void {
  actions$
    .pipe(ofType(...successTypes), take(1), takeUntilDestroyed(destroyRef))
    .subscribe(onSuccess);
  actions$
    .pipe(ofType(...failTypes), take(1), takeUntilDestroyed(destroyRef))
    .subscribe((action) => onError((action as { error: unknown }).error));
}

interface ModalFormComponent {
  form: FormGroup;
  submitting: () => boolean;
  submit: () => void;
}

export const saveFooter = <T,>(
  translate: TranslateService,
): ModalButtonOptions<T>[] => [
  {
    label: translate.instant('common.save'),
    type: 'primary',
    onClick: (form) => {
      const f = form as ModalFormComponent | undefined;
      if (f?.form.valid && !f.submitting()) {
        f.submit();
      }
    },
  },
];
