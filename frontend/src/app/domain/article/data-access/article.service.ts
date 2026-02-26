import { inject, Injectable } from '@angular/core';
import { Article, ArticleRequest } from '@domain/article/models/article.model';
import { environment } from '@env/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { ApiRes, ResponsePage } from '@core/models/common.model';

@Injectable({
  providedIn: 'root',
})
export class ArticleService {
  URL = `${environment.apiUrl}/article`;
  private http = inject(HttpClient);

  getAll(query?: any) {
    let params = new HttpParams();
    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params = params.set(key, value.toString());
        }
      });
    }
    return this.http
      .get<ApiRes<ResponsePage<Article[]>>>(`${this.URL}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<Article[]>>('article')),
      );
  }

  getOne(id: number) {
    return this.http.get<ApiRes<Article>>(`${this.URL}/${id}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<Article>('article')),
    );
  }

  add(article: ArticleRequest) {
    return this.http.post<ApiRes<Article>>(`${this.URL}`, article).pipe(
      map((res) => res.data),
      catchError(this.handleError<Article>('article')),
    );
  }

  update(id: number, article: ArticleRequest) {
    return this.http.put<ApiRes<Article>>(`${this.URL}/${id}`, article).pipe(
      map((res) => res.data),
      catchError(this.handleError<Article>('article')),
    );
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.URL}/${id}`).pipe(
      map((res) => console.log(res)),
      catchError(this.handleError<void>('article')),
    );
  }
  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.log(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
