import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes } from '@core/models/common.model';
import {
  SeatTemplate,
  SeatTemplateRequest,
} from '@domain/theater/models/seat-template.model';

@Injectable({
  providedIn: 'root',
})
export class SeatLayoutService {
  URL = `${environment.apiUrl}/rooms`;
  private http = inject(HttpClient);

  getByRoom(roomId: number) {
    return this.http
      .get<ApiRes<SeatTemplate[]>>(`${this.URL}/${roomId}/seats`)
      .pipe(
        map((res) => res.data ?? []),
        catchError(this.handleError<SeatTemplate[]>('seat layout')),
      );
  }

  replaceLayout(roomId: number, seats: SeatTemplateRequest[]) {
    return this.http
      .put<ApiRes<SeatTemplate[]>>(`${this.URL}/${roomId}/layout`, seats)
      .pipe(
        map((res) => res.data ?? []),
        catchError(this.handleError<SeatTemplate[]>('seat layout')),
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed:`, error);
      return throwError(() => error);
    };
  }
}
