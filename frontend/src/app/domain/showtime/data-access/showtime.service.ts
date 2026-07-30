import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes, ResponsePage } from '@core/models/common.model';
import {
  Showtime,
  ShowtimeRequest,
} from '@domain/showtime/models/showtime.model';

@Injectable({
  providedIn: 'root',
})
export class ShowtimeService {
  private http = inject(HttpClient);
  URL_SHOWTIME = `${environment.apiUrl}/showtime`;

  // Showtime
  getShowtimeByDate(date: number) {
    const params = new HttpParams().set('date', date);
    return this.http
      .get<ApiRes<Showtime[]>>(`${this.URL_SHOWTIME}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Showtime[]>('showtime')),
      );
  }

  getShowtimeByMovie(movieId: number, query?: any) {
    let params = new HttpParams();
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params = params.set(key, value.toString());
        }
      });
    }
    return this.http
      .get<
        ApiRes<ResponsePage<Showtime[]>>
      >(`${this.URL_SHOWTIME}/movies/${movieId}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<Showtime[]>>('showtime')),
      );
  }

  getShowtimeByTheater(theaterId: number, query?: any) {
    let params = new HttpParams();
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params = params.set(key, value.toString());
        }
      });
    }
    return this.http
      .get<
        ApiRes<ResponsePage<Showtime[]>>
      >(`${this.URL_SHOWTIME}/theaters/${theaterId}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<Showtime[]>>('showtime')),
      );
  }

  getAvailableShowtime(date: Date) {
    const params = new HttpParams().set('date', date.toDateString());
    return this.http
      .get<ApiRes<Showtime[]>>(`${this.URL_SHOWTIME}/available`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Showtime[]>('showtime')),
      );
  }

  getAvailableShowtimeByMovie(movieId: number) {
    return this.http
      .get<
        ApiRes<Showtime[]>
      >(`${this.URL_SHOWTIME}/available/movies/${movieId}`)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Showtime[]>('showtime')),
      );
  }

  getShowtime(id: number) {
    return this.http.get<ApiRes<Showtime>>(`${this.URL_SHOWTIME}/${id}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<Showtime>('showtime')),
    );
  }

  createShowtime(showtime: ShowtimeRequest) {
    return this.http
      .post<ApiRes<Showtime>>(`${this.URL_SHOWTIME}`, showtime)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Showtime>('showtime')),
      );
  }

  updateShowtime(id: number, showtime: ShowtimeRequest) {
    return this.http
      .put<ApiRes<Showtime>>(`${this.URL_SHOWTIME}/${id}`, showtime)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Showtime>('showtime')),
      );
  }

  deleteShowtime(id: number) {
    return this.http
      .delete<void>(`${this.URL_SHOWTIME}/${id}`)
      .pipe(catchError(this.handleError<void>('showtime')));
  }
  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
