import { SeatStatus } from '@core/constants/enum';
import { Showtime } from '@domain/showtime/models/showtime.model';
import { Reservation } from '@domain/reservation/models/reservation.model';

export interface Seat {
  id: number;
  showtimeId: number;
  seatNumber: string;
  seatType: string;
  status: SeatStatus;
}

export interface SeatRequest {
  id: number;
  showtime: Showtime;
  seatNumber: string;
  status: SeatStatus;
  reservation: Reservation;
}
