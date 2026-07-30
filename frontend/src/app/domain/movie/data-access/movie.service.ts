import { inject, Injectable } from '@angular/core';
import {
  HttpClient,
  HttpContext,
  HttpHeaders,
  HttpParams,
} from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes, ResponsePage } from '@core/models/common.model';
import { Movie, MovieRequest } from '@domain/movie/models/movie.model';
import { SKIP_AUTH, USE_HEADER } from '@core/constants/http-context.tokens';

@Injectable({
  providedIn: 'root',
})
export class MovieService {
  URL = `${environment.apiUrl}/movie`;
  private apiMovie = environment.tmbd.apiUrl;
  private headers = new HttpHeaders({
    Authorization: `Bearer ${environment.tmbd.token}`,
  });
  private http = inject(HttpClient);

  getMovieLists(query?: any) {
    let params = new HttpParams();
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params = params.set(key, value.toString());
        }
      });
    }
    return this.http
      .get<ApiRes<ResponsePage<Movie[]>>>(`${this.URL}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<Movie[]>>('movie')),
      );
  }

  saveMovie(movie: MovieRequest) {
    return this.http.post<ApiRes<Movie>>(`${this.URL}`, movie).pipe(
      map((res) => res.data),
      catchError(this.handleError<Movie>('movie')),
    );
  }

  findOneMovie(id: number) {
    return this.http.get<ApiRes<Movie>>(`${this.URL}/${id}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<Movie>('movie')),
    );
  }

  updateMovie(id: number, movie: MovieRequest) {
    return this.http.put<ApiRes<Movie>>(`${this.URL}/${id}`, movie).pipe(
      map((res) => res.data),
      catchError(this.handleError<Movie>('movie')),
    );
  }

  removeMovie(id: number) {
    return this.http.delete<void>(`${this.URL}/${id}`).pipe(
      catchError(this.handleError<void>('movie')),
    );
  }

  searchMovies(query: string, language = 'vi-VN') {
    return this.http
      .get(
        `${this.apiMovie}/search/movie?query=${encodeURIComponent(query)}&language=${language}&page=1`,
        {
          headers: this.headers,
          context: new HttpContext()
            .set(SKIP_AUTH, true)
            .set(USE_HEADER, false),
        },
      )
      .pipe(map((res: any) => res.results || []));
  }

  getMovieDetails(id: number, language = 'vi-VN') {
    return this.http.get(`${this.apiMovie}/movie/${id}?language=${language}`, {
      headers: this.headers,
      context: new HttpContext().set(SKIP_AUTH, true).set(USE_HEADER, false),
    });
  }

  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
