import {
  DateModel,
  FilterDate,
  FilterQuery,
  FilterSort,
  IsDeleted,
} from '@core/models/common.model';
import { User } from '@domain/user/models/user.model';
import { Showtime } from '@domain/showtime/models/showtime.model';
import { ReservationStatus } from '@core/constants/enum';
import { Seat } from '@domain/seat/models/seat.model';

export interface Reservation extends DateModel, IsDeleted {
  id: string;
  user: User;
  showtime: Showtime;
  reservationStatus: ReservationStatus;
  totalAmount: number;
  seats: Seat[];
}

export interface ReservationInitRequest {
  reservationId?: string;
  userId: number;
  showtimeId: number;
  theaterId: number;
}

export interface ReservationInitResponse {
  reservationCode: string;
  /** @deprecated Legacy BE returned reservationId; kept for mock compat. */
  reservationId?: string;
  showtimeId: number;
  ttl: number;
  expiredAt: Date;
}

export interface ReservationRequest {
  reservationId: string;
  userId: number;
  showtimeId: number;
  seatIds: number[];
}

export interface ReservationFilter extends FilterQuery, FilterSort, FilterDate {
  userId: number;
  reservationStatus: ReservationStatus;
  totalAmount: number;
}

export interface ReservationProfile {
  reservationId: string;
  moviePoster: string;
  movieTitle: string;
  room: string;
  date: string;
  time: string;
  theater: string;
  seats: string[];
  amount: number;
}
