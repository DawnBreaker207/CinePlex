import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes } from '@core/models/common.model';
import { Room, RoomRequest } from '@domain/theater/models/room.model';

@Injectable({
  providedIn: 'root',
})
export class RoomService {
  URL = `${environment.apiUrl}/rooms`;
  private http = inject(HttpClient);

  getByTheater(theaterId: number) {
    return this.http
      .get<ApiRes<Room[]>>(`${this.URL}/theaters/${theaterId}`)
      .pipe(
        map((res) => res.data ?? []),
        catchError(this.handleError<Room[]>('rooms')),
      );
  }

  create(theaterId: number, room: RoomRequest) {
    return this.http
      .post<ApiRes<Room>>(`${this.URL}/theaters/${theaterId}`, room)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Room>('room')),
      );
  }

  update(id: number, room: RoomRequest) {
    return this.http.put<ApiRes<Room>>(`${this.URL}/${id}`, room).pipe(
      map((res) => res.data),
      catchError(this.handleError<Room>('room')),
    );
  }

  remove(id: number) {
    return this.http
      .delete<void>(`${this.URL}/${id}`)
      .pipe(catchError(this.handleError<void>('room')));
  }

  private handleError<T>(operation = 'operation') {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed:`, error);
      return throwError(() => error);
    };
  }
}
