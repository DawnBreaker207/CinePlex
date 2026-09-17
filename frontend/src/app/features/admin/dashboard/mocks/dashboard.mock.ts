import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
dayjs.extend(customParseFormat);
import {
  DashboardMetrics,
  PaymentDistribution,
  RevenuePoint,
  TopMovie,
  TopTheater,
} from '@domain/dashboard/models/dashboard.model';

export interface Operation {
  id: string;
  type: 'FAILED_PAYMENT' | 'REFUND_REQUEST' | 'LOW_OCCUPANCY';
  title: string;
  detail: string;
  amount?: number;
  time: string;
}

export interface MockSummary {
  metrics: DashboardMetrics;
  previousMetrics: DashboardMetrics;
  revenues: RevenuePoint[];
  movies: TopMovie[];
  theaters: TopTheater[];
  payments: PaymentDistribution[];
  operations: Operation[];
}

const MOVIES = [
  'Gió Mùa Hè',
  'Sát Thủ Vô Hình',
  'Ngày Xưa Có Một Chuyện',
  'Chuyện Tình Giữa Lòng',
  'Ánh Trăng Không Ngủ',
  'Người Bạn Đặc Biệt',
  'Cơn Mưa Cuối Hạ',
  'Hành Trình Sống Còn',
  'Người Thừa Kế',
  'Đêm Trong Thành Phố',
];

const THEATERS = [
  'Rạp Alpha Center',
  'Rạp Beta Mall',
  'Rạp Gamma Plaza',
  'Rạp Delta City',
  'Rạp Epsilon Park',
  'Rạp Zeta Avenue',
];

const PAYMENT_METHODS: [string, number][] = [
  ['VNPay', 0.45],
  ['MoMo', 0.25],
  ['ZaloPay', 0.18],
  ['Cash', 0.12],
];

const dayjsParse = (d: string) => dayjs(d, 'DD/MM/YYYY');

function wave(index: number, period: number, phase: number): number {
  return Math.sin((index / period) * Math.PI * 2 + phase);
}

function dailyRevenue(start: dayjs.Dayjs, days: number): RevenuePoint[] {
  return Array.from({ length: days }, (_, i) => {
    const base = 18_000_000;
    const weekly = wave(i, 7, -1.2);
    const growth = 1 + (i / Math.max(days, 1)) * 0.25;
    const noise = wave(i, 3.7, 0.8) * 0.08;
    return {
      date: start.add(i, 'day').toDate(),
      revenue: Math.round(base * (1 + weekly * 0.35 + noise) * growth),
    };
  });
}

function topMovies(revenue: number, count: number): TopMovie[] {
  return MOVIES.slice(0, count).map((name, i) => {
    const share = 0.14 - i * 0.009;
    const movieRevenue = Math.round(revenue * Math.max(share, 0.015));
    return {
      movieName: name,
      ticketSold: Math.round(movieRevenue / 90_000),
      revenue: movieRevenue,
    };
  });
}

function topTheaters(revenue: number, count: number): TopTheater[] {
  return THEATERS.slice(0, count).map((name, i) => {
    const share = 0.2 - i * 0.018;
    const theaterRevenue = Math.round(revenue * Math.max(share, 0.03));
    return {
      theaterName: name,
      ticketsSold: Math.round(theaterRevenue / 88_000),
      totalRevenue: theaterRevenue,
    };
  });
}

const OPERATION_TITLES = {
  FAILED_PAYMENT: [
    'VNPay hết thời hạn xác thực',
    'MoMo từ chối giao dịch',
    'Thẻ hết hạn sử dụng',
  ],
  REFUND_REQUEST: [
    'Khách yêu cầu hoàn vé do lịch chiếu thay đổi',
    'Khách trả vé trước giờ chiếu 3 giờ',
  ],
  LOW_OCCUPANCY: [
    'Suất chiếu trống trên 80% ghế',
    'Buổi chiếu sáng vắng khách',
  ],
} as const;

function operations(): Operation[] {
  const now = dayjs();
  const ops: Operation[] = [];
  OPERATION_TITLES.FAILED_PAYMENT.forEach((title, i) => {
    ops.push({
      id: `fp-${i}`,
      type: 'FAILED_PAYMENT',
      title,
      detail: MOVIES[i % MOVIES.length],
      amount: 120_000 + i * 45_000,
      time: now.subtract(i * 2 + 1, 'hour').format('DD/MM/YYYY HH:mm'),
    });
  });
  OPERATION_TITLES.REFUND_REQUEST.forEach((title, i) => {
    ops.push({
      id: `rf-${i}`,
      type: 'REFUND_REQUEST',
      title,
      detail: MOVIES[(i + 2) % MOVIES.length],
      amount: 90_000 + i * 30_000,
      time: now.subtract(i * 3 + 2, 'hour').format('DD/MM/YYYY HH:mm'),
    });
  });
  OPERATION_TITLES.LOW_OCCUPANCY.forEach((title, i) => {
    ops.push({
      id: `lo-${i}`,
      type: 'LOW_OCCUPANCY',
      title,
      detail: THEATERS[i % THEATERS.length],
      time: now.subtract(i * 5 + 4, 'hour').format('DD/MM/YYYY HH:mm'),
    });
  });
  return ops;
}

export function getMockSummary(payload: {
  startDate?: string;
  endDate?: string;
}): Promise<MockSummary> {
  const start = dayjsParse(payload.startDate || dayjs().format('DD/MM/YYYY'));
  const end = dayjsParse(payload.endDate || start.format('DD/MM/YYYY'));
  const days = Math.max(1, end.diff(start, 'day') + 1);

  const revenues = dailyRevenue(start, Math.min(days, 92));
  const totalRevenue = revenues.reduce((sum, r) => sum + r.revenue, 0);
  const ticketsSold = Math.round(totalRevenue / 90_000);
  const previousTotal = Math.round(totalRevenue * (0.9 + wave(0, 5, 0) * 0.05));

  const metrics: DashboardMetrics = {
    totalRevenue,
    ticketsSold,
    activeTheaters: THEATERS.length,
    seatUtilization: Math.round(58 + wave(0, 3, 1) * 12),
  };

  const previousMetrics: DashboardMetrics = {
    totalRevenue: previousTotal,
    ticketsSold: Math.round(previousTotal / 90_000),
    activeTheaters: THEATERS.length - 1,
    seatUtilization: Math.max(10, metrics.seatUtilization - 4),
  };

  const payments: PaymentDistribution[] = PAYMENT_METHODS.map(
    ([method, share]) => ({
      method,
      amount: Math.round(totalRevenue * share),
    }),
  );

  return new Promise((resolve) => {
    setTimeout(
      () =>
        resolve({
          metrics,
          previousMetrics,
          revenues,
          movies: topMovies(totalRevenue, 5),
          theaters: topTheaters(totalRevenue, 5),
          payments,
          operations: operations(),
        }),
      350 + Math.random() * 300,
    );
  });
}
