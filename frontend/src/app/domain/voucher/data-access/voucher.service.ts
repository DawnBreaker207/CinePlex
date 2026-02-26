import { inject, Injectable } from '@angular/core';
import { environment } from '@env/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ApiRes, ResponsePage } from '@core/models/common.model';
import { catchError, map, Observable, throwError } from 'rxjs';
import {
  Voucher,
  VoucherCalculate,
  VoucherRequest,
} from '@domain/voucher/model/voucher.model';

@Injectable({
  providedIn: 'root',
})
export class VoucherService {
  URL = `${environment.apiUrl}/voucher`;
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
      .get<ApiRes<ResponsePage<Voucher[]>>>(`${this.URL}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<ResponsePage<Voucher[]>>('voucher')),
      );
  }

  getOne(id: number) {
    return this.http.get<ApiRes<Voucher>>(`${this.URL}/${id}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<Voucher>('voucher')),
    );
  }

  add(voucher: VoucherRequest) {
    return this.http.post<ApiRes<Voucher>>(`${this.URL}`, voucher).pipe(
      map((res) => res.data),
      catchError(this.handleError<Voucher>('voucher')),
    );
  }

  update(id: number, voucher: VoucherRequest) {
    return this.http.put<ApiRes<Voucher>>(`${this.URL}/${id}`, voucher).pipe(
      map((res) => res.data),
      catchError(this.handleError<Voucher>('voucher')),
    );
  }

  delete(id: number) {
    return this.http
      .delete<void>(`${this.URL}/${id}`)
      .pipe(catchError(this.handleError<void>('voucher')));
  }

  calculateDiscount(code: string, total: number) {
    let params = new HttpParams();
    params = params.set('code', code);
    params = params.set('total', total.toString());
    return this.http
      .get<ApiRes<VoucherCalculate>>(`${this.URL}/calculate`, { params })
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<VoucherCalculate>('voucher')),
      );
  }
  private handleError<T>(operation = 'operation') {
    return (error: any): Observable<T> => {
      console.log(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
