import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes } from '@core/models/common.model';
import { Payment, PaymentRequest } from '@domain/payment/models/payment.model';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  URL = `${environment.apiUrl}/payment`;
  private http = inject(HttpClient);

  createPayment(query: PaymentRequest) {
    let params = new HttpParams();
    params = params.set('reservationId', query.reservationId);
    params = params.set('amount', query.amount);
    params = params.set('paymentType', query.paymentType);
    return this.http
      .get<ApiRes<Payment>>(`${this.URL}/create`, { params })
      .pipe(
        map((res) => res.data.paymentUrl),
        catchError(this.handleError<string>('Payment')),
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
