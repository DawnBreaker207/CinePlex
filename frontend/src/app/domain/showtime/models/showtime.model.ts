import { DateModel, IsDeleted } from '@core/models/common.model';

export interface Showtime extends DateModel, IsDeleted {
  id: number;
  movieId: number;
  movieTitle: string;
  moviePosterUrl: string;

  theaterId: number;
  theaterName: string;
  theaterLocation: string;

  showDate: Date;
  showTime: Date;

  price: number;

  totalSeats: number;
  availableSeats: number;
  roomName?: string;
}

export interface ShowtimeRequest {
  movieId: number;
  theaterId: number;
  showDate: Date;
  showtime: Date;
  price: number;
  totalSeats: number;
}
