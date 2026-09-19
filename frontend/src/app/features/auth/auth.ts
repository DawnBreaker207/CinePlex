import { Component, DestroyRef, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { matchValidator } from '@shared/utils/validators';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { NzAlertComponent } from 'ng-zorro-antd/alert';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzInputDirective, NzInputGroupComponent } from 'ng-zorro-antd/input';
import { NzFormControlComponent, NzFormDirective, NzFormItemComponent, NzFormLabelComponent } from 'ng-zorro-antd/form';
import { Store } from '@ngrx/store';
import { Router, RouterLink } from '@angular/router';
import { Actions, ofType } from '@ngrx/effects';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, take } from 'rxjs';
import { NzCheckboxComponent } from 'ng-zorro-antd/checkbox';
import { NzStringTemplateOutletDirective } from 'ng-zorro-antd/core/outlet';
import { AuthActions } from '@core/auth/auth.actions';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { errorMessage } from '@shared/utils/error.helper';
import { selectAuthError } from '@core/auth/auth.selectors';

@Component({
  selector: 'app-auth',
  imports: [
    FormsModule, ReactiveFormsModule, RouterLink, TranslatePipe,
    NzButtonComponent, NzAlertComponent, NzIconDirective,
    NzToolTipModule,
    NzInputDirective, NzInputGroupComponent,
    NzFormControlComponent, NzFormDirective, NzFormItemComponent, NzFormLabelComponent,
    NzCheckboxComponent, NzStringTemplateOutletDirective,
  ],
  templateUrl: './auth.html',
})
export class AuthComponent implements OnInit {
  activeTab = signal<'login' | 'register'>('login');
  loginError = signal<string | null>(null);
  registerError = signal<string | null>(null);
  registeredEmail = signal<string | null>(null);
  loginPasswordVisible = false;
  registerPasswordVisible = false;
  isLoginLoading = signal(false);
  isRegisterLoading = signal(false);

  private fb = inject(NonNullableFormBuilder);
  private store = inject(Store);
  private router = inject(Router);
  private actions$ = inject(Actions);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

  formLogin!: FormGroup<{
    identifier: FormControl<string>;
    password: FormControl<string>;
    remember: FormControl<boolean>;
  }>;
  formRegister!: FormGroup<{
    email: FormControl<string>;
    username: FormControl<string>;
    password: FormControl<string>;
    confirmPassword: FormControl<string>;
  }>;
  private firstInput = viewChild<ElementRef<HTMLInputElement>>('firstInput');

  ngOnInit() {
    this.initializeLoginForm();
    this.initializeRegisterForm();
    this.activeTab.set(this.router.url.includes('register') ? 'register' : 'login');

    this.formRegister.get('password')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.formRegister.get('confirmPassword')?.updateValueAndValidity();
    });

    this.store.select(selectAuthError).pipe(
      filter(Boolean),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((err: unknown) => {
      const msg = errorMessage(err) || this.translate.instant('auth.genericError');
      if (this.activeTab() === 'login') this.loginError.set(msg);
      else this.registerError.set(msg);
    });
  }

  toggleMode() {
    const next = this.activeTab() === 'login' ? 'register' : 'login';
    this.activeTab.set(next);
    this.loginError.set(null);
    this.registerError.set(null);
    if (next === 'login') {
      this.formLogin.reset({ remember: true });
    } else {
      this.formRegister.reset();
    }
    setTimeout(() => this.firstInput()?.nativeElement?.focus(), 100);
  }

  private initializeLoginForm() {
    this.formLogin = this.fb.group({
      identifier: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      remember: [false],
    });
  }

  private initializeRegisterForm() {
    this.formRegister = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, matchValidator('password')]],
    });
  }

  submitLogin() {
    if (this.formLogin.valid) {
      this.isLoginLoading.set(true);
      this.loginError.set(null);
      const { identifier, password } = this.formLogin.getRawValue();
      this.store.dispatch(AuthActions.loadLogin({ user: { identifier, password } }));
      this.actions$.pipe(ofType(AuthActions.loadLoginSuccess, AuthActions.loadLoginFailure), take(1), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
        this.isLoginLoading.set(false);
      });
    } else {
      this.markInvalid(this.formLogin);
    }
  }

  submitRegister() {
    if (this.formRegister.valid) {
      this.isRegisterLoading.set(true);
      this.registerError.set(null);
      const { username, email, password } = this.formRegister.getRawValue();
      this.store.dispatch(AuthActions.loadRegister({ user: { username, email, password } }));

      this.actions$.pipe(
        ofType(AuthActions.loadRegisterSuccess, AuthActions.loadRegisterFailure),
        take(1), takeUntilDestroyed(this.destroyRef),
      ).subscribe((action) => {
        this.isRegisterLoading.set(false);
        if (action.type === AuthActions.loadRegisterSuccess.type) {
          this.activeTab.set('login');
          this.formLogin.patchValue({ identifier: email });
          this.loginError.set(null);
          this.registeredEmail.set(email);
        }
      });
    } else {
      this.markInvalid(this.formRegister);
    }
  }



  private markInvalid(form: FormGroup) {
    Object.values(form.controls).forEach((control) => {
      if (control.invalid) {
        control.markAsDirty();
        control.updateValueAndValidity({ onlySelf: true });
      }
    });
  }
}
