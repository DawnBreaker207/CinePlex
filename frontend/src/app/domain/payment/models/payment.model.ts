import { PaymentMethod, PaymentStatus } from '@core/constants/enum';

export interface Payment {
  code: string;
  message: string;
  paymentUrl: string;
}

export interface PaymentRequest {
  reservationId: string;
  amount: number;
  paymentType: PaymentMethod;
  // Paid-seat snapshot for post-payment reconciliation (BE compares these
  // against the held seats; a mismatch fails safe into the refund queue).
  seatIds?: number[];
}

export interface PaymentRefund {
  reservationId: string;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  statusReason: string | null;
  paidAt: string;
  createdAt: string;
}
