import { CanMatchFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Role } from '@core/constants/enum';

export const RedirectGuard: CanMatchFn = (_route, _segments) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  if (
    authService.isAuthenticated() &&
    authService.hasRole(Role.ADMIN.toString())
  ) {
    return router.parseUrl('/admin');
  }
  return true;
};
