import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const matchValidator = (matchTo: string): ValidatorFn =>
  (control: AbstractControl): ValidationErrors | null => {
    const match = control.parent?.get(matchTo);
    return match && control.value !== match.value ? { match: true } : null;
  };

export const phoneValidator: ValidatorFn =
  (control: AbstractControl): ValidationErrors | null => {
    const valid = /^(\+84|0)[3-9][0-9]{8}$/.test(control.value);
    return valid ? null : { phone: true };
  };

export const urlValidator: ValidatorFn =
  (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    try {
      new URL(control.value);
      return null;
    } catch {
      return { url: true };
    }
  };
