import { FilterDate } from '@core/models/common.model';

export type DashboardQuery = FilterDate;

export interface DashboardMetrics {
  totalRevenue: number;
  ticketsSold: number;
  activeTheaters: number;
  seatUtilization: number;
}

export interface PaymentDistribution {
  method: string;
  amount: number;
}

export interface TopMovie {
  movieName: string;
  ticketSold: number;
  revenue: number;
}

export interface TopTheater {
  theaterName: string;
  ticketsSold: number;
  totalRevenue: number;
}

export interface RevenuePoint {
  date: Date;
  revenue: number;
}

export interface DashboardSummary {
  metrics: DashboardMetrics;
  revenues: RevenuePoint[];
  movies: TopMovie[];
  theaters: TopTheater[];
  payments: PaymentDistribution[];
}

export interface DailyForecast {
  date: Date;
  dayOfWeek: string;
  projectedRevenue: number;
  sampleDays: number;
}
