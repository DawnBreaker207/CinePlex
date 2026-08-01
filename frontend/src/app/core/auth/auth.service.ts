import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiRes } from '../models/common.model';
import { Jwt, JWTPayload, RefreshToken } from '../models/jwt.model';
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  tap,
  throwError,
} from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '@env/environment';
import { LoginRequest, RegisterRequest } from '@domain/user/models/user.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // TTL: access token 10 phút (<= 15 phút), refresh token 7 ngày (<= 30 ngày).
  // Token KHÔNG bao giờ vào localStorage — access token chỉ ở trong memory signal,
  // refresh token nằm ở httpOnly cookie do backend quản lý.
  private readonly ACCESS_TTL_MIN = 10;
  private readonly REFRESH_TTL_DAYS = 7;

  private http = inject(HttpClient);
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

  login(request: LoginRequest) {
    return this.http.post<ApiRes<Jwt>>(`${this.URL}/login`, request).pipe(
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
        { withCredentials: true },
      )
      .pipe(
        map((res) => res.data),
        tap((token) => {
          this._accessToken.set(token.accessToken);
        }),
        catchError(this.handleError<RefreshToken>('refreshToken')),
      );
  }

  /**
   * Silent refresh (dedupe bằng shareReplay(1)): mọi caller đồng thời
   * (app initializer, interceptor 401, guard) chia sẻ đúng MỘT request refresh.
   * Không bao giờ throw — fail thì trả null để caller quyết định (logout).
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

  hasRole(role: string) {
    const user = this.getCurrentUser();
    return user?.roles.includes(role) || false;
  }

  hasAnyRole(roles: string[]) {
    const user = this.getCurrentUser();
    if (!user || !user.roles) return false;

    return roles.some((role) => user.roles.includes(role));
  }

  private handleError<T>(operation = 'operation', _result?: T) {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
