import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes, ResponsePage } from '@core/models/common.model';
import { AuditFilter, AuditLog } from '@domain/audit/models/audit.model';

@Injectable({
  providedIn: 'root',
})
export class AuditService {
  URL = `${environment.apiUrl}/audit-logs`;
  private http = inject(HttpClient);

  search(filter: AuditFilter) {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http
      .get<ApiRes<ResponsePage<AuditLog[]>>>(this.URL, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<AuditLog[]>>('audit logs')),
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed:`, error);
      return throwError(() => error);
    };
  }
}
