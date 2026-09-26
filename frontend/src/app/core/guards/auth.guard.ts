import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { ToastService } from '@core/services/toast/toast.service';
import { AuthService } from '@core/auth/auth.service';
import { TranslateService } from '@ngx-translate/core';
import { Observable, map } from 'rxjs';

export const AuthGuard: CanMatchFn = (_route, _segments): Observable<boolean | UrlTree> => {
  const toast = inject(ToastService);
  const router = inject(Router);
  const authService = inject(AuthService);
  const translate = inject(TranslateService);

  const deny = () => {
    toast.createNotification({
      type: 'error',
      title: translate.instant('guard.authRequired.title'),
      message: translate.instant('guard.authRequired.message'),
    });
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: router.url },
    });
  };

  // One lazy refresh attempt (valid cookie in, expired back to login).
  // Public pages never reach here so guests cost no call.
  return authService
    .ensureAuthenticated()
    .pipe(map((ok) => (ok ? true : deny())));
};
