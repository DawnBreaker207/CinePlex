import { inject, Injectable } from '@angular/core';
import { environment } from '@env/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ApiRes } from '@core/models/common.model';
import { PermissionKey } from '@core/constants/enum';
import { catchError, map, Observable, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class PermissionService {
  URL = `${environment.apiUrl}/permissions`;
  private http = inject(HttpClient);

  list(userId: number) {
    return this.http
      .get<ApiRes<PermissionKey[]>>(`${this.URL}/user/${userId}`)
      .pipe(
        map((res) => res.data ?? []),
        catchError(this.handleError<PermissionKey[]>()),
      );
  }

  grant(userId: number, key: PermissionKey) {
    const params = new HttpParams().set('userId', userId).set('key', key);
    return this.http
      .post<ApiRes<PermissionKey[]>>(`${this.URL}/grant`, null, { params })
      .pipe(
        map((res) => res.data ?? []),
        catchError(this.handleError<PermissionKey[]>()),
      );
  }

  revoke(userId: number, key: PermissionKey) {
    const params = new HttpParams().set('userId', userId).set('key', key);
    return this.http
      .post<ApiRes<PermissionKey[]>>(`${this.URL}/revoke`, null, { params })
      .pipe(
        map((res) => res.data ?? []),
        catchError(this.handleError<PermissionKey[]>()),
      );
  }

  private handleError<T>() {
    return (error: unknown): Observable<T> => {
      console.error(`permission failed:`, error);
      return throwError(() => error);
    };
  }
}