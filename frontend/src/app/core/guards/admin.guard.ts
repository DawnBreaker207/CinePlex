import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { ToastService } from '@core/services/toast/toast.service';
import { AuthService } from '@core/auth/auth.service';
import { Role } from '@core/constants/enum';
import { TranslateService } from '@ngx-translate/core';
import { Observable, map, of } from 'rxjs';
import { environment } from '@env/environment';

export const AdminGuard: CanMatchFn = (_route, _segments): Observable<boolean | UrlTree> => {
  // TEMP-TEST: bypass auth for local testing — remove before commit
  if (environment.bypassAuth) return of(true);
  const toast = inject(ToastService);
  const router = inject(Router);
  const authService = inject(AuthService);
  const translate = inject(TranslateService);

  return authService.ensureAuthenticated().pipe(
    map((ok) => {
      if (!ok) {
        toast.createNotification({
          type: 'error',
          title: translate.instant('guard.authRequired.title'),
          message: translate.instant('guard.authRequired.message'),
          position: 'topRight',
        });
        return router.parseUrl('/login');
      }

      if (!authService.hasRole(Role.ADMIN.toString())) {
        toast.createNotification({
          type: 'error',
          title: translate.instant('guard.accessDenied.title'),
          message: translate.instant('guard.accessDenied.message'),
          position: 'topRight',
        });
        return router.parseUrl('/home');
      }

      return true;
    }),
  );
};
