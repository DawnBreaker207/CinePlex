import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes, ResponsePage } from '@core/models/common.model';
import { Theater, TheaterRequest } from '@domain/theater/models/theater.model';

@Injectable({
  providedIn: 'root',
})
export class TheaterService {
  URL_THEATER = `${environment.apiUrl}/theater`;

  private http = inject(HttpClient);

  // Theater
  getTheaters(query?: any) {
    let params = new HttpParams();
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params = params.set(key, value.toString());
        }
      });
    }
    return this.http
      .get<ApiRes<ResponsePage<Theater[]>>>(`${this.URL_THEATER}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<Theater[]>>('theater')),
      );
  }

  getTheaterByLocation(location: string) {
    const params = new HttpParams().set('location', location);
    return this.http
      .get<
        ApiRes<ResponsePage<Theater[]>>
      >(`${this.URL_THEATER}/search`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<Theater[]>>('theater')),
      );
  }

  getTheater(id: number) {
    return this.http.get<ApiRes<Theater>>(`${this.URL_THEATER}/${id}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<Theater>('theater')),
    );
  }

  createTheater(theater: TheaterRequest) {
    return this.http.post<ApiRes<Theater>>(`${this.URL_THEATER}`, theater).pipe(
      map((res) => res.data),
      catchError(this.handleError<Theater>('theater')),
    );
  }

  updateTheater(id: number, schedule: TheaterRequest) {
    return this.http
      .put<ApiRes<Theater>>(`${this.URL_THEATER}/${id}`, schedule)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Theater>('theater')),
      );
  }

  deleteTheater(id: number) {
    return this.http
      .delete<void>(`${this.URL_THEATER}/${id}`)
      .pipe(catchError(this.handleError<void>('theater')));
  }

  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
