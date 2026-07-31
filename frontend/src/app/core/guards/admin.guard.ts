import { CanMatchFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { ToastService } from '@core/services/toast/toast.service';
import { AuthService } from '@core/auth/auth.service';
import { Role } from '@core/constants/enum';
import { TranslateService } from '@ngx-translate/core';

export const AdminGuard: CanMatchFn = (_route, _segments) => {
  const toast = inject(ToastService);
  const router = inject(Router);
  const authService = inject(AuthService);
  const translate = inject(TranslateService);

  if (!authService.isAuthenticated()) {
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
};
