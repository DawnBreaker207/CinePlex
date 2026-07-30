import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { ApiRes } from '@core/models/common.model';
import { Seat } from '@domain/seat/models/seat.model';
import { catchError, map, Observable, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SeatService {
  private http = inject(HttpClient);
  URL_SEAT = `${environment.apiUrl}/seats`;

  // Seat
  getSeatByShowtime(showtimeId: number) {
    return this.http
      .get<ApiRes<Seat[]>>(`${this.URL_SEAT}/showtime/${showtimeId}`)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Seat[]>('showtime')),
      );
  }

  getAvailableSeatByShowtime(showtimeId: number) {
    return this.http
      .get<ApiRes<Seat[]>>(`${this.URL_SEAT}/showtime/${showtimeId}/available`)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Seat[]>('showtime')),
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
