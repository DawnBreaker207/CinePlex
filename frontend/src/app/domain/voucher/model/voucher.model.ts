import { DateModel } from '@core/models/common.model';

export enum VoucherCategory {
  CAMPAIGN = 'CAMPAIGN',
  SYSTEM = 'SYSTEM',
}

export enum DiscountType {
  FIXED = 'FIXED',
  PERCENT = 'PERCENT',
}

export type VoucherStatus =
  | 'SCHEDULED'
  | 'ACTIVE'
  | 'PAUSED'
  | 'EXPIRED'
  | 'EXHAUSTED';

export interface Voucher extends DateModel {
  id: number;
  name: string;
  code: string;
  startAt: Date;
  endAt: Date;
  quantityTotal: number;
  quantityUsed: number;
  category: VoucherCategory;
  groupRef: string;
  conditions: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount: number;
  minOrderValue: number;
  status: VoucherStatus;
  maxPerUser: number;
  isActive: boolean;
}

export interface VoucherRequest {
  name: string;
  code: string;
  quantityTotal: number;
  category: VoucherCategory;
  groupRef: string;
  conditions: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount: number;
  minOrderValue: number;
  startAt: Date;
  endAt: Date;
  status: VoucherStatus;
  maxPerUser: number;
}

export interface VoucherCalculate {
  code: string;
  originalAmount: number;
  discountAmount: number;
  finalAmount: number;
}

export type UserVoucherStatus = 'AVAILABLE' | 'USED' | 'EXPIRED';

export interface UserVoucher {
  id: number;
  code: string;
  voucherName: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount: number;
  minOrderValue: number;
  status: UserVoucherStatus;
  expiredAt: Date;
}
