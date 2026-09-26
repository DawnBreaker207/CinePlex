export type ReservationStatus = 'CONFIRMED' | 'CANCELED' | 'PENDING' | 'FAILED' | 'EXPIRED' | 'REFUNDED';

export type PaymentMethod = 'MOMO' | 'VNPAY' | 'ZALOPAY' | 'CASH' | 'MOCK';

export type PaymentStatus = 'PENDING' | 'PAID' | 'CANCELED' | 'REFUND_PENDING' | 'REFUNDED';

export type PermissionKey = 'CONTENT' | 'PRICING' | 'FINANCE' | 'ACCOUNTS';

export const PERMISSION_KEY_LABELS: Record<PermissionKey, string> = {
  CONTENT: 'permission.key.content',
  PRICING: 'permission.key.pricing',
  FINANCE: 'permission.key.finance',
  ACCOUNTS: 'permission.key.accounts',
};

export type SeatStatus = 'AVAILABLE' | 'BOOKED' | 'SELECTED' | 'HOLD';

export type Mode = 'add' | 'edit' | 'view';

export enum Role {
  USER = 'USER',
  MODERATOR = 'MODERATOR',
  ADMIN = 'ADMIN',
  OWNER = 'OWNER',
}
