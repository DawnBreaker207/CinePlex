import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '@env/environment';
import {
  Reservation,
  ReservationFilter,
  ReservationInitRequest,
  ReservationInitResponse,
  ReservationProfile,
  ReservationRequest,
} from '@domain/reservation/models/reservation.model';
import { formatDate } from '@shared/utils/date.helper';
import { ApiRes, ResponsePage } from '@core/models/common.model';
import { ReservationStatus } from '@core/constants/enum';

export interface LockedSeat {
  seatId: number;
  reservationId: string;
}

export interface VoucherDiscount {
  code: string;
  originalAmount: number;
  discountAmount: number;
  finalAmount: number;
}

export interface CounterSaleRequest {
  showtimeId: number;
  seatIds: number[];
  paymentMethod: string;
  customerName?: string;
  customerPhone?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ReservationService {
  URL = `${environment.apiUrl}/reservation`;
  private http = inject(HttpClient);

  getReservations(
    filter?: Partial<ReservationFilter>,
    page: number = 0,
    size: number = 0,
  ) {
    let params = new HttpParams();

    params = params.set('page', page.toString());
    params = params.set('size', size.toString());

    const startDate = formatDate(filter?.startDate);
    const endDate = formatDate(filter?.endDate);
    filter = { ...filter, startDate: startDate, endDate: endDate };

    if (filter) {
      Object.keys(filter).forEach((key) => {
        const value = filter[key as keyof ReservationFilter];
        if (value !== null && value !== undefined && value !== '') {
          params = params.set(key, value.toString());
        }
      });
    }

    return this.http
      .get<ApiRes<ResponsePage<Reservation[]>>>(`${this.URL}`, { params })
      .pipe(
        map((res) => res.data),
        catchError(
          this.handleError<ResponsePage<Reservation[]>>('Get reservations'),
        ),
      );
  }

  getReservation(id: string) {
    return this.http.get<ApiRes<Reservation>>(`${this.URL}/${id}`).pipe(
      map((res) => res.data),
      catchError(this.handleError<Reservation>('Get reservation')),
    );
  }

  checkIn(code: string) {
    return this.http
      .post<ApiRes<Reservation>>(`${this.URL}/${code}/checkin`, {})
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Reservation>('Check in')),
      );
  }

  getUserReservation(id: number, status: ReservationStatus = 'CONFIRMED') {
    const params = new HttpParams().set('userId', id).set('status', status);
    return this.http
      .get<
        ApiRes<ResponsePage<ReservationProfile[]>>
      >(`${this.URL}/me`, { params })
      .pipe(
        map((res) => res.data),
        catchError(
          this.handleError<ResponsePage<ReservationProfile[]>>(
            'Reservation Profile',
          ),
        ),
      );
  }

  initReservation(reservation: ReservationInitRequest) {
    return this.http
      .post<ApiRes<ReservationInitResponse>>(`${this.URL}/init`, reservation)
      .pipe(
        map((res) => res.data),
        catchError(
          this.handleError<ReservationInitResponse>('Init reservation'),
        ),
      );
  }

  holdReservationSeat(reservation: ReservationRequest) {
    return this.http
      .post<ApiRes<void>>(`${this.URL}/seatHold`, reservation)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<void>('hold seat reservation')),
      );
  }

  restoreReservation(reservationId: string) {
    return this.http
      .get<
        ApiRes<ReservationInitResponse>
      >(`${this.URL}/${reservationId}/restore`)
      .pipe(
        map((res) => res.data),
        catchError(
          this.handleError<ReservationInitResponse>('Restore reservation'),
        ),
      );
  }

  getLockedSeats(showtimeId: number) {
    return this.http
      .get<LockedSeat[]>(`${this.URL}/showtimes/${showtimeId}/locked-seats`)
      .pipe(
        map((res) => res ?? []),
        catchError(this.handleError<LockedSeat[]>('Locked seats')),
      );
  }

  applyVoucher(reservationId: string, code: string) {
    return this.http
      .post<ApiRes<VoucherDiscount>>(
        `${this.URL}/${reservationId}/voucher`,
        null,
        { params: { code } },
      )
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<VoucherDiscount>('Apply voucher')),
      );
  }

  counterSale(request: CounterSaleRequest) {
    return this.http
      .post<ApiRes<Reservation>>(`${this.URL}/staff/counter-sale`, request)
      .pipe(
        map((res) => res.data),
        catchError(this.handleError<Reservation>('Counter ticket sale')),
      );
  }

  private handleError<T>(operation = 'operation') {
    return (error: unknown): Observable<T> => {
      console.error(`${operation} failed: ${error}`);
      return throwError(() => error);
    };
  }
}
