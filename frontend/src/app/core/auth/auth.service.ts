import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { SKIP_ERROR_NOTIFICATION } from '@core/constants/http-context.tokens';
import { ApiRes } from '../models/common.model';
import { Jwt, JWTPayload, RefreshToken } from '../models/jwt.model';
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '@env/environment';
import { LoginRequest, RegisterRequest } from '@domain/user/models/user.model';
import { Store } from '@ngrx/store';
import { AuthActions } from './auth.actions';
import { UserService } from '@domain/user/data-access/user.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // TTLs stay within backend limits (access <= 15m, refresh <= 30d).
  // Tokens never touch localStorage: access lives in a memory signal,
  // refresh lives in a backend-managed httpOnly cookie.
  private readonly ACCESS_TTL_MIN = 10;
  private readonly REFRESH_TTL_DAYS = 7;

  private http = inject(HttpClient);
  private store = inject(Store);
  private userService = inject(UserService);
  URL = `${environment.apiUrl}/auth`;

  private _accessToken = signal<string | null>(null);
  readonly accessToken = this._accessToken.asReadonly();

  private refreshInFlight$: Observable<string | null> | null = null;

  register(request: RegisterRequest) {
    return this.http.post<ApiRes<string>>(`${this.URL}/register`, request).pipe(
      map((res) => res.data),
      catchError(this.handleError<string>('register')),
    );
  }

  verifyEmail(token: string) {
    return this.http
      .get<ApiRes<string>>(`${this.URL}/verify-email`, { params: { token } })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<string>('verifyEmail')),
      );
  }

  resendVerification(email: string) {
    return this.http
      .post<ApiRes<string>>(
        `${this.URL}/resend-verification`,
        null,
        { params: { email } },
      )
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<string>('resendVerification')),
      );
  }

  login(request: LoginRequest) {
    // withCredentials: accepts the httpOnly refresh cookie; without it F5 loses the session.
    return this.http
      .post<ApiRes<Jwt>>(`${this.URL}/login`, request, {
        withCredentials: true,
      })
      .pipe(
        map((res) => res?.data),
        tap((jwt) => {
          if (jwt?.accessToken) this._accessToken.set(jwt.accessToken);
        }),
        catchError(this.handleError<Jwt>('login')),
      );
  }

  logout() {
    return this.http
      .post<void>(`${this.URL}/logout`, {}, { withCredentials: true })
      .pipe(
        finalize(() => this._accessToken.set(null)),
        catchError(this.handleError<void>('logout')),
      );
  }

  setAccessToken(token: string | null) {
    this._accessToken.set(token);
  }

  callRefreshToken() {
    return this.http
      .post<ApiRes<RefreshToken>>(
        `${this.URL}/refresh-token`,
        {},
        {
          withCredentials: true,
          context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
        },
      )
      .pipe(
        map((res) => res.data),
        tap((token) => this._accessToken.set(token.accessToken)),
        // After F5 the store is empty: resolve the real userId via the token email
        // (JWT sub is username, not numeric) before syncing ngrx.
        // Best effort: header-level login state survives regardless.
        switchMap((token) => {
          const email = this.getFromToken(token.accessToken)?.email;
          if (!email) return of(token);
          return this.userService.getByEmail(email).pipe(
            tap((user) => {
              if (user) {
                this.store.dispatch(
                  AuthActions.loadLoginSuccess({
                    jwt: {
                      userId: user.userId,
                      username: user.username ?? '',
                      email: user.email ?? '',
                      accessToken: token.accessToken,
                    },
                  }),
                );
              }
            }),
            map(() => token),
            catchError(() => of(token)),
          );
        }),
        catchError(this.handleError<RefreshToken>('refreshToken')),
      );
  }

  /**
   * Silent refresh deduped by shareReplay(1): concurrent callers share ONE refresh request.
   * Never throws — returns null on failure and lets the caller decide (logout).
   */
  silentRefresh(): Observable<string | null> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.callRefreshToken().pipe(
        map((token) => token?.accessToken ?? null),
        catchError(() => of(null)),
        shareReplay(1),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
      );
    }
    return this.refreshInFlight$;
  }

  getFromToken(token: string) {
    if (!token) return null;
    try {
      return jwtDecode<JWTPayload>(token);
    } catch (error) {
      console.error('Invalid token', error);
      return null;
    }
  }

  getCurrentUser() {
    const token = this._accessToken();
    if (!token) return null;
    return this.getFromToken(token);
  }

  isTokenExpired(token?: string): boolean {
    const currentUser = token ?? this._accessToken();
    if (!currentUser) return true;

    const payload = this.getFromToken(currentUser);
    if (!payload?.exp) return true;
    return payload.exp < Math.floor(Date.now() / 1000);
  }

  isAuthenticated(): boolean {
    const token = this._accessToken();
    return !!token && !this.isTokenExpired(token);
  }

  /**
   * For guards: memory token short-circuits; otherwise one lazy httpOnly-cookie refresh.
   */
  ensureAuthenticated(): Observable<boolean> {
    if (this.isAuthenticated()) return of(true);
    return this.silentRefresh().pipe(map((token) => !!token));
  }

  hasRole(role: string) {
    const user = this.getCurrentUser();
    return user?.roles?.includes(role) || false;
  }

  hasAnyRole(roles: string[]) {
    const user = this.getCurrentUser();
    if (!user || !user.roles) return false;

    return roles.some((role) => user.roles?.includes(role) || false);
  }

  private handleError<T>(operation = 'operation', _result?: T) {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
