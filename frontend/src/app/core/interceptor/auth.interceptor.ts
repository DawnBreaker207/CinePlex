import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { Store } from '@ngrx/store';
import { SKIP_AUTH, USE_HEADER } from '@core/constants/http-context.tokens';
import { AuthActions } from '@core/auth/auth.actions';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const AuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const store = inject(Store);

  const token = authService.accessToken();
  const skipAuth = req.context.get(SKIP_AUTH);
  const useHeader = req.context.get(USE_HEADER);
  const isRefresh = req.url.includes('/refresh-token');

  let modifiedReq = req;
  const checkApi = req.url.startsWith('/api') || req.url.includes('/api/v1');

  if (checkApi) {
    modifiedReq = req.clone({ withCredentials: true });
  }

  // Refresh-token request uses the httpOnly cookie only, never the access token
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

      // TOKEN_EXPIRED (or no message) → silent refresh + single retry.
      // TOKEN_INVALID/revoked → logout immediately, no retry.
      const message =
        typeof error.error?.message === 'string'
          ? error.error.message.toLowerCase()
          : '';
      const tokenInvalid =
        message.includes('not found') ||
        message.includes('invalid') ||
        message.includes('revoked');

      if (tokenInvalid) {
        isRefreshing = false;
        authService.setAccessToken(null);
        store.dispatch(AuthActions.loadLogout());
        return throwError(() => error);
      }

      // Single-flight refresh token queue (inspired by production Cinema project)
      if (isRefreshing) {
        return refreshTokenSubject.pipe(
          filter((t): t is string => t !== null),
          take(1),
          switchMap((newToken) => {
            return next(
              req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` },
                withCredentials: true,
              }),
            );
          }),
        );
      }

      isRefreshing = true;
      refreshTokenSubject.next(null);

      return authService.silentRefresh().pipe(
        switchMap((newToken) => {
          isRefreshing = false;
          if (!newToken) {
            authService.setAccessToken(null);
            store.dispatch(AuthActions.loadLogout());
            refreshTokenSubject.next(null);
            return throwError(() => error);
          }
          refreshTokenSubject.next(newToken);
          return next(
            req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` },
              withCredentials: true,
            }),
          );
        }),
        catchError((refreshErr) => {
          isRefreshing = false;
          authService.setAccessToken(null);
          store.dispatch(AuthActions.loadLogout());
          refreshTokenSubject.next(null);
          return throwError(() => refreshErr);
        }),
      );
    }),
  );
};
