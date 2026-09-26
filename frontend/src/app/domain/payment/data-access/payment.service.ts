import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import { ApiRes } from '@core/models/common.model';
import { Payment, PaymentRefund, PaymentRequest } from '@domain/payment/models/payment.model';

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
    for (const seatId of query.seatIds ?? []) {
      params = params.append('seatIds', seatId);
    }
    return this.http
      .get<ApiRes<Payment>>(`${this.URL}/create`, { params })
      .pipe(
        map((res) => res.data.paymentUrl),
        catchError(this.handleError<string>('Payment')),
      );
  }

  getRefundPending() {
    return this.http.get<ApiRes<PaymentRefund[]>>(`${this.URL}/refunds/pending`).pipe(
      map((res) => res.data ?? []),
      catchError(this.handleError<PaymentRefund[]>('Refund requests')),
    );
  }

  getFailedPayments() {
    return this.http.get<ApiRes<PaymentRefund[]>>(`${this.URL}/failed`).pipe(
      map((res) => res.data ?? []),
      catchError(this.handleError<PaymentRefund[]>('Failed payments')),
    );
  }

  rejectRefund(reservationId: string) {
    return this.http
      .post<ApiRes<string>>(`${this.URL}/refunds/${reservationId}/reject`, {})
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<string>('Reject refund')),
      );
  }

  approveRefund(reservationId: string) {
    return this.http
      .post<ApiRes<string>>(`${this.URL}/refunds/${reservationId}/approve`, {})
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<string>('Approve refund')),
      );
  }

  reCheck(provider: string, id: string) {
    return this.http
      .post<ApiRes<string>>(`${this.URL}/re-check/${provider}/${id}`, {})
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<string>('Re-check payment')),
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
