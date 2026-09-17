import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';
import { Store } from '@ngrx/store';
import { SKIP_AUTH, USE_HEADER } from '@core/constants/http-context.tokens';
import { AuthActions } from '@core/auth/auth.actions';

export const AuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const store = inject(Store);

  const token = authService.accessToken();
  const skipAuth = req.context.get(SKIP_AUTH);
  const useHeader = req.context.get(USE_HEADER);
  const isRefresh = req.url.includes('/refresh-token');

  let modifiedReq = req;
  const checkApi = req.url.startsWith('/api');

  if (checkApi) {
    modifiedReq = req.clone({ withCredentials: true });
  }

  // Refresh-token request chỉ dùng cookie httpOnly, KHÔNG attach access token
  if (token && !skipAuth && !isRefresh) {
    modifiedReq = modifiedReq.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  if (skipAuth || useHeader) {
    return next(modifiedReq);
  }

  return next(modifiedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) return throwError(() => error);
      if (req.url.includes('/refresh-token')) return throwError(() => error);
      if (req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
        return throwError(() => error);
      }

      // TOKEN_EXPIRED (hoặc không có message) → silent refresh + retry 1 lần.
      // TOKEN_INVALID/revoked → logout ngay, không retry.
      const message =
        typeof error.error?.message === 'string'
          ? error.error.message.toLowerCase()
          : '';
      const tokenInvalid = message.includes('not found') || message.includes('invalid') || message.includes('revoked');
      if (tokenInvalid) {
        authService.setAccessToken(null);
        store.dispatch(AuthActions.loadLogout());
        return throwError(() => error);
      }

      return authService.silentRefresh().pipe(
        switchMap((newToken) => {
          if (!newToken) {
            authService.setAccessToken(null);
            store.dispatch(AuthActions.loadLogout());
            return throwError(() => error);
          }
          return next(
            req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` },
              withCredentials: true,
            }),
          );
        }),
      );
    }),
  );
};
