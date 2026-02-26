import { inject, Injectable } from '@angular/core';
import { environment } from '@env/environment';
import { HttpClient } from '@angular/common/http';
import { ApiRes } from '@core/models/common.model';
import { catchError, map, Observable, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ChatboxService {
  URL = `${environment.apiUrl}/ai`;
  private http = inject(HttpClient);

  sendMessage(message: string) {
    return this.http.post<ApiRes<string>>(`${this.URL}/chat`, { message }).pipe(
      map((res) => res.data),
      catchError(this.handleError<string>('chatbox')),
    );
  }
  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.log(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
