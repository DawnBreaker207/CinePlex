import { inject, Injectable } from '@angular/core';
import {
  Actions,
  createEffect,
  ofType,
  ROOT_EFFECTS_INIT,
} from '@ngrx/effects';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import { ToastService } from '@core/services/toast/toast.service';
import { AuthActions } from '@core/auth/auth.actions';

@Injectable()
export class AuthEffects {
  private actions$ = inject(Actions);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private translate = inject(TranslateService);
  private router = inject(Router);

  private toast(key: string, type: 'success' | 'error' = 'success') {
    this.toastService.createNotification({
      title: this.translate.instant('auth.toast.success'),
      message: this.translate.instant(key),
      type,
    });
  }

  initAuth$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(ROOT_EFFECTS_INIT),
      switchMap(() => {
        const existing = this.authService.accessToken();
        // No eager refresh here: httpOnly cookie is unreadable from JS,
        // blind calls 404 for every guest. Guard/interceptor refresh lazily.
        if (!existing) return of(AuthActions.loadLogoutSuccess());
        return of(existing).pipe(
          map((accessToken) => {
            const payload = this.authService.getFromToken(accessToken);
            if (!payload) return AuthActions.loadLogoutSuccess();
            return AuthActions.loadLoginSuccess({
              jwt: {
                userId: Number(payload.sub),
                username: payload.username ?? '',
                email: payload.email ?? '',
                accessToken,
              },
            });
          }),
        );
      }),
    );
  });

  register$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.loadRegister),
      switchMap(({ user }) =>
        this.authService.register(user).pipe(
          tap(() => {
            this.toast('auth.toast.registerSuccess');
          }),
          map((token) => AuthActions.loadRegisterSuccess({ token })),
          catchError((err) =>
            of(AuthActions.loadRegisterFailure({ error: err })),
          ),
        ),
      ),
    );
  });

  login$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.loadLogin),
      switchMap(({ user }) =>
        this.authService.login(user).pipe(
          tap(() => {
            this.toast('auth.toast.loginSuccess');
            this.router.navigate(['/home']);
          }),
          map((jwt) => {
            if (!jwt || !jwt.accessToken) {
              console.error(
                '[login$] invalid jwt from service, dispatching failure:',
                jwt,
              );
              return AuthActions.loadLoginFailure({
                error: 'Invalid JWT response',
              });
            }
            return AuthActions.loadLoginSuccess({ jwt });
          }),
          catchError((err) =>
            of(AuthActions.loadLoginFailure({ error: err })),
          ),
        ),
      ),
    );
  });

  logout$ = createEffect(() => {
    return this.actions$.pipe(
      ofType(AuthActions.loadLogout),
      switchMap(() =>
        this.authService.logout().pipe(
          tap(() => {
            this.toast('auth.toast.logoutSuccess');
            this.router.navigate(['/home']);
          }),
          map(() => AuthActions.loadLogoutSuccess()),
          catchError((err) =>
            of(AuthActions.loadLogoutFailure({ error: err })),
          ),
        ),
      ),
    );
  });
}
