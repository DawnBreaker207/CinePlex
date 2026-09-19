export type ReservationStatus = 'CONFIRMED' | 'CANCELED' | 'PENDING' | 'FAILED' | 'EXPIRED' | 'REFUNDED';

export type PaymentMethod = 'MOMO' | 'VNPAY' | 'ZALOPAY' | 'CASH' | 'MOCK';

export type PaymentStatus = 'PENDING' | 'PAID' | 'CANCELED';

export type SeatStatus = 'AVAILABLE' | 'BOOKED' | 'SELECTED' | 'HOLD';

export type Mode = 'add' | 'edit' | 'view';

export enum Role {
  USER = 'USER',
  MODERATOR = 'MODERATOR',
  ADMIN = 'ADMIN',
}
