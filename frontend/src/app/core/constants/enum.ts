export type ReservationStatus = 'CONFIRMED' | 'CANCELED';

export type PaymentMethod = 'MOMO' | 'VNPAY' | 'ZALOPAY';

export type PaymentStatus = 'PENDING' | 'PAID' | 'CANCELED';

export type SeatStatus = 'AVAILABLE' | 'BOOKED' | 'SELECTED' | 'HOLD';

export type Mode = 'add' | 'edit' | 'view';

export enum Role {
  USER = 'USER',
  MODERATOR = 'MODERATOR',
  ADMIN = 'ADMIN',
}
