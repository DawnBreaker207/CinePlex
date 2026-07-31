import { AbstractControl, FormGroup } from '@angular/forms';
import { Actions, ofType } from '@ngrx/effects';
import { ActionCreator } from '@ngrx/store';
import { NzModalRef } from 'ng-zorro-antd/modal';
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
  successTypes: ActionCreator[],
  failTypes: ActionCreator[],
  onSuccess: () => void,
  onError: (error: any) => void,
): void {
  actions$
    .pipe(ofType(...successTypes), take(1), takeUntilDestroyed())
    .subscribe(onSuccess);
  actions$
    .pipe(ofType(...failTypes), take(1), takeUntilDestroyed())
    .subscribe((action) => onError((action as { error: any }).error));
}

interface ModalFormComponent {
  form: FormGroup;
  submitting: () => boolean;
  submit: () => void;
}

export const saveFooter = (
  modal: NzModalRef,
): { label: string; type: 'primary'; onClick: () => void }[] => [
  {
    label: 'Lưu',
    type: 'primary',
    onClick: () => {
      const form = modal.getContentComponent() as ModalFormComponent;
      if (form.form.valid && !form.submitting()) {
        form.submit();
      }
    },
  },
];
