import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiRes } from '../models/common.model';
import { Jwt, JWTPayload, RefreshToken } from '../models/jwt.model';
import { catchError, map, Observable, tap, throwError } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { StorageService } from '@core/services/storage/storage.service';
import { environment } from '@env/environment';
import { LoginRequest, RegisterRequest } from '@domain/user/models/user.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private storageService = inject(StorageService);
  URL = `${environment.apiUrl}/auth`;

  register(request: RegisterRequest) {
    return this.http.post<ApiRes<string>>(`${this.URL}/register`, request).pipe(
      map((res) => res.data),
      catchError(this.handleError<string>('register')),
    );
  }

  login(request: LoginRequest) {
    return this.http.post<ApiRes<Jwt>>(`${this.URL}/login`, request).pipe(
      tap((res) => console.log('[auth.service] login response:', res)),
      map((res) => {
        console.log('[auth.service] res.data:', res?.data);
        return res?.data;
      }),
      catchError(this.handleError<Jwt>('login')),
    );
  }

  logout() {
    return this.http
      .post<void>(`${this.URL}/logout`, {}, { withCredentials: true })
      .pipe(catchError(this.handleError<Jwt>('logout')));
  }

  callRefreshToken() {
    return this.http
      .post<
        ApiRes<RefreshToken>
      >(`${this.URL}/refresh-token`, {}, { withCredentials: true })
      .pipe(
        map((res) => res.data),
        tap((token) => {
          this.storageService.setItem('accessToken', token.accessToken);
        }),
        catchError(this.handleError<RefreshToken>('refreshToken')),
      );
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
    const token = this.storageService.getItem('accessToken') as string;
    if (!token) return null;
    return this.getFromToken(token);
  }

  isTokenExpired(token?: string): boolean {
    const currentUser = token || this.storageService.getItem('accessToken');
    if (!currentUser) return true;

    const payload = this.getFromToken(currentUser);
    if (!payload?.exp) return true;
    return payload.exp < Math.floor(Date.now() / 1000);
  }

  isAuthenticated(): boolean {
    const token = this.storageService.getItem('accessToken') as string;
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
