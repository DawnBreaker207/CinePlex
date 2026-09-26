import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { User, UserProfile } from '@domain/user/models/user.model';
import { ApiRes, FilterQuery, FilterSort, ResponsePage } from '@core/models/common.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  URL = `${environment.apiUrl}/user`;
  private http = inject(HttpClient);

  getAll(filter?: FilterSort & FilterQuery) {
    let params = new HttpParams();
    if (filter) {
      Object.keys(filter).forEach((key) => {
        const value = filter[key as keyof typeof filter];
        if (value !== null && value !== undefined && value !== '') {
          params = params.set(key, value.toString());
        }
      });
    }
    return this.http.get<ApiRes<ResponsePage<User[]>>>(`${this.URL}`, { params }).pipe(
      map((res) => res.data),
      catchError(this.handleError<ResponsePage<User[]>>('Get users')),
    );
  }

  getById(id: number) {
    return this.http.get<ApiRes<User>>(`${this.URL}/${id}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<User>('Get users')),
    );
  }

  getByEmail(email: string) {
    return this.http.get<ApiRes<User>>(`${this.URL}/email/${email}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<User>('Get users')),
    );
  }

  updateInfo(id: number, info: UserProfile) {
    return this.http
      .put<ApiRes<User>>(`${this.URL}/update/${id}/profile`, info)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<User>('Update user status')),
      );
  }

  updateStatus(id: number, status: boolean) {
    return this.http
      .put<ApiRes<User>>(`${this.URL}/update/${id}/status`, status)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<User>('Update user status')),
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
