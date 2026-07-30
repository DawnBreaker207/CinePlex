import { HttpEvent, HttpHandlerFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import {
  MOCK_THEATERS, MOCK_MOVIES, getShowtimes,
  MOCK_SEATS, MOCK_USERS, getJwtPayload, MOCK_JWT_TOKEN, MOCK_RESERVATION_INIT, MOCK_RESERVATION,
} from './mock-data';

function ok(data: unknown) {
  return of(new HttpResponse({ status: 200, body: { code: 'OK', message: 'success', data } }));
}

function paginated(data: unknown) {
  return ok({ content: data, pagination: { page: 0, size: 100, totalElements: Array.isArray(data) ? data.length : 0, totalPages: 1 } });
}

export function mockInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const { method, url } = req;

  if (url.includes('/api/v1/auth/login') && method === 'POST') {
    const body = req.body as any;
    console.log('[mock] login request body:', body);
    const { identifier, password } = body;
    const user = MOCK_USERS.find(u => u.email === identifier && u.password === password);
    console.log('[mock] found user:', user);
    if (!user) return of(new HttpResponse({ status: 401, body: { code: 'ERROR', message: 'Sai email hoặc mật khẩu' } }));
    const jwtData = { userId: user.id, username: user.name, email: user.email, accessToken: MOCK_JWT_TOKEN };
    console.log('[mock] returning jwt:', jwtData);
    return ok(jwtData);
  }

  if (url.includes('/api/v1/auth/register') && method === 'POST') {
    return ok('Tài khoản đã được tạo thành công');
  }

  if (url.includes('/api/v1/auth/logout') && method === 'POST') {
    return ok(null);
  }

  if (url.includes('/api/v1/auth/refresh-token') && method === 'POST') {
    return ok({ accessToken: MOCK_JWT_TOKEN });
  }

  if (url.includes('/api/v1/theater/search') && method === 'GET') {
    const location = req.params?.get('location')?.toLowerCase() || '';
    return paginated(MOCK_THEATERS.filter(t => t.name.toLowerCase().includes(location) || t.location.toLowerCase().includes(location)));
  }

  if (url.match(/\/api\/v1\/theater\/\d+$/) && method === 'GET') {
    const id = parseInt(url.split('/').pop()!);
    return ok(MOCK_THEATERS.find(t => t.id === id) || null);
  }

  if (url.includes('/api/v1/theater') && method === 'GET') {
    return paginated(MOCK_THEATERS);
  }

  if (url.match(/\/api\/v1\/movie\/\d+$/) && method === 'GET') {
    const id = parseInt(url.split('/').pop()!);
    return ok(MOCK_MOVIES.find(m => m.id === id) || null);
  }

  if (url.includes('/api/v1/movie') && method === 'GET') {
    return paginated(MOCK_MOVIES);
  }

  if (url.includes('/api/v1/showtime/available/movies/') && method === 'GET') {
    const movieId = parseInt(url.split('/').pop()!);
    const all = getShowtimes(1).concat(getShowtimes(2)).concat(getShowtimes(3));
    return ok(all.filter((st: any) => st.movieId === movieId));
  }

  if (url.includes('/api/v1/showtime/theaters/') && method === 'GET') {
    const theaterId = parseInt(url.match(/theaters\/(\d+)/)?.[1] || '1');
    return paginated(getShowtimes(theaterId));
  }

  if (url.includes('/api/v1/showtime/available') && method === 'GET') {
    return ok(getShowtimes(1));
  }

  if (url.match(/\/api\/v1\/showtime\/\d+$/) && method === 'GET') {
    const id = parseInt(url.split('/').pop()!);
    return ok(getShowtimes(1).find((st: any) => st.id === id) || null);
  }

  if (url.includes('/api/v1/showtime') && method === 'GET') {
    return ok(getShowtimes(1));
  }

  if (url.includes('/api/v1/seats/showtime/') && method === 'GET') {
    return ok(MOCK_SEATS);
  }

  if (url.includes('/api/v1/reservation/init') && method === 'POST') {
    return ok(MOCK_RESERVATION_INIT);
  }

  if (url.includes('/api/v1/reservation/seatHold') && method === 'POST') {
    return ok(null);
  }

  if (url.includes('/api/v1/reservation/me') && method === 'GET') {
    return paginated([{
      id: 'mock-res-1', movieTitle: 'Kẻ Trộm Mặt Trăng 4',
      moviePoster: 'https://image.tmdb.org/t/p/w500/l3zC6FSDz2TBcZ3FUlOqmlCT6Oc.jpg',
      theaterName: 'CGV Vincom Center', showDate: new Date(), showTime: '19:00',
      seats: ['D4', 'D5'], totalPrice: 130000, status: 'CONFIRMED',
      ticketCode: 'TICKET-MOCK-001', createdAt: new Date(),
    }]);
  }

  if (url.match(/\/api\/v1\/reservation\/.+?\/restore/) && method === 'GET') {
    return ok(MOCK_RESERVATION_INIT);
  }

  if (url.match(/\/api\/v1\/reservation\/\d+$/) && method === 'GET') {
    return ok(MOCK_RESERVATION);
  }

  if (url.includes('/api/v1/reservation') && method === 'GET') {
    return paginated([MOCK_RESERVATION]);
  }

  if (url.includes('/api/v1/payment/create') && method === 'GET') {
    const reservationId = req.params?.get('reservationId') || 'mock-res-1';
    const success = Math.random() > 0.3;
    return ok({ paymentUrl: `/paymentResult?status=${success ? 'success' : 'error'}&reservationId=${reservationId}` });
  }

  if (url.includes('/api/v1/user/email/') && method === 'GET') {
    return ok({ id: 1, email: 'admin@cineplex.vn', name: 'Admin', role: 'ADMIN', status: 'ACTIVE' });
  }

  if (url.match(/\/api\/v1\/user\/\d+$/) && method === 'GET') {
    return ok({ id: 1, email: 'admin@cineplex.vn', name: 'Admin', role: 'ADMIN', status: 'ACTIVE' });
  }

  if (url.includes('/api/v1/user') && method === 'GET') {
    return paginated([{ id: 1, email: 'admin@cineplex.vn', name: 'Admin', role: 'ADMIN', status: 'ACTIVE' }]);
  }

  if (url.includes('/api/v1/user/update') && method === 'PUT') {
    return ok({ id: 1, email: 'admin@cineplex.vn', name: 'Admin', role: 'ADMIN', status: 'ACTIVE' });
  }

  if (url.includes('/api/v1/ai/chat') && method === 'POST') {
    return ok('Xin chào! Tôi là trợ lý ảo của CinePlex. Tôi có thể giúp gì cho bạn?');
  }

  if (url.includes('/api/v1/cloudinary/upload') && method === 'POST') {
    return ok({ secure_url: 'https://placehold.co/300x400/DBEAFE/1F2937?text=Uploaded' });
  }

  if (url.includes('/api/v1/voucher/calculate') && method === 'GET') {
    const code = req.params?.get('code') || '';
    const discounts: Record<string, any> = {
      WELCOME10: { code: 'WELCOME10', discountValue: 10000, discountType: 'FIXED', maxDiscountAmount: 10000, minOrderValue: 0 },
      CINEPLEX20: { code: 'CINEPLEX20', discountValue: 20000, discountType: 'FIXED', maxDiscountAmount: 20000, minOrderValue: 50000 },
      NEWUSER: { code: 'NEWUSER', discountValue: 15, discountType: 'PERCENTAGE', maxDiscountAmount: 30000, minOrderValue: 0 },
    };
    const result = discounts[code];
    if (!result) return of(new HttpResponse({ status: 400, body: { code: 'ERROR', message: 'Mã giảm giá không hợp lệ' } }));
    return ok(result);
  }

  if (url.includes('/api/v1/voucher') && method === 'GET') {
    return paginated([]);
  }

  if (url.includes('/api/v1/dashboard/summary') && method === 'GET') {
    return ok({ totalRevenue: 150000000, totalTickets: 2340, totalMovies: 5, totalTheaters: 3, totalUsers: 128 });
  }

  if (url.includes('/api/v1/dashboard/metrics') && method === 'GET') {
    return ok({ revenueGrowth: 12.5, ticketGrowth: 8.3, avgOccupancy: 67 });
  }

  if (url.includes('/api/v1/dashboard/revenue') && method === 'GET') {
    return ok(Array.from({ length: 12 }, (_, i) => ({ date: `2026-${String(i + 1).padStart(2, '0')}`, revenue: 80000000 + Math.random() * 50000000 })));
  }

  if (url.includes('/api/v1/dashboard/top-movies') && method === 'GET') {
    return ok(MOCK_MOVIES.slice(0, 3).map((m, i) => ({ movieTitle: m.title, ticketCount: 500 - i * 100, revenue: 30000000 - i * 5000000 })));
  }

  if (url.includes('/api/v1/dashboard/top-theaters') && method === 'GET') {
    return ok(MOCK_THEATERS.map(t => ({ theaterName: t.name, ticketCount: 300 + Math.floor(Math.random() * 200), revenue: 20000000 + Math.random() * 15000000 })));
  }

  if (url.includes('/api/v1/dashboard/payment-distribution') && method === 'GET') {
    return ok([{ paymentMethod: 'MOMO', count: 45 }, { paymentMethod: 'VNPAY', count: 30 }, { paymentMethod: 'ATM', count: 25 }]);
  }

  if (url.includes('/api/v1/article') && method === 'GET') {
    return paginated([]);
  }

  if (url.includes('/api/v1/report/export/') && method === 'GET') {
    return of(new HttpResponse({ status: 200, body: new Blob(['mock,csv,data'], { type: 'text/csv' }) }));
  }

  return next(req);
}
