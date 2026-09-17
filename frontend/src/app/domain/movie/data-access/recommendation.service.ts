import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes } from '@core/models/common.model';
import { Movie } from '@domain/movie/models/movie.model';

@Injectable({
  providedIn: 'root',
})
export class RecommendationService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/ai/recommendations`;

  recommend(limit = 6): Observable<Movie[]> {
    return this.http
      .get<ApiRes<Movie[]>>(this.url, { params: { limit } })
      .pipe(
        map((res) => res.data ?? []),
        catchError(() => of([])),
      );
  }
}
