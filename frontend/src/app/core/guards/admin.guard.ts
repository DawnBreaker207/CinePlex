import { CanMatchFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { ToastService } from '@core/services/toast/toast.service';
import { AuthService } from '@core/auth/auth.service';
import { Role } from '@core/constants/enum';

export const AdminGuard: CanMatchFn = (_route, _segments) => {
  const toast = inject(ToastService);
  const router = inject(Router);
  const authService = inject(AuthService);

  if (!authService.isAuthenticated()) {
    toast.createNotification({
      type: 'error',
      title: 'Authentication Required',
      message: 'Please login to access this page',
      position: 'topRight',
    });
    return router.parseUrl('/login');
  }

  if (!authService.hasRole(Role.ADMIN.toString())) {
    toast.createNotification({
      type: 'error',
      title: 'Access Denied',
      message: 'You do not have permission to go to this page',
      position: 'topRight',
    });
    return router.parseUrl('/home');
  }

  return true;
};
