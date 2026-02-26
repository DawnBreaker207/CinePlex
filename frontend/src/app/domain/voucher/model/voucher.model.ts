import { DateModel } from '@core/models/common.model';

export enum VoucherCategory {
  CAMPAIGN = 'CAMPAIGN',
  SYSTEM = 'SYSTEM',
}

export enum DiscountType {
  FIXED = 'FIXED',
  PERCENT = 'PERCENT',
}

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
}

export interface VoucherCalculate {
  code: string;
  discountAmount: number;
  finalAMount: number;
}
