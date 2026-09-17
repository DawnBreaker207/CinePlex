import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Observable, delay, of, throwError } from 'rxjs';
import {
  MOCK_ARTICLES,
  MOCK_DASHBOARD_SUMMARY,
  MOCK_MOVIES,
  MOCK_RESERVATIONS,
  MOCK_THEATERS,
  MOCK_USERS,
  MOCK_VOUCHERS,
  addUser,
  findByCredential,
  getSeats,
  getShowtimes,
  publicUsers,
  toReservationProfile,
  type MockUser,
  VOUCHER_CALCULATIONS,
} from './mock-data';

type MockBody = Record<string, unknown>;

const pushMock = <T>(list: T[], item: unknown): void => {
  list.push(item as T);
};

function b64url(input: string): string {
  return btoa(unescape(encodeURIComponent(input)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function mockJwt(user: MockUser): string {
  const payload = {
    sub: user.userId,
    username: user.username,
    email: user.email,
    roles: [user.role],
    exp: Math.floor(Date.now() / 1000) + 600, // access token TTL 10 phút (<= 15 phút)
  };
  const header = { alg: 'none', typ: 'JWT' };
  return `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(payload))}.mock-sig`;
}

/* ---- mock refresh token: simulate httpOnly cookie (JS không set được httpOnly, chỉ mock) ---- */
const RT_COOKIE = 'refreshToken';
// ponytail: path '/' thay vì '/api/v1/auth/refresh-token' — cookie path-scoped
// không đọc được từ page khác path (karma chạy ở '/'); BE thật vẫn dùng path scope.
const RT_PATH = '/';
const RT_TTL_MS = 7 * 24 * 3600 * 1000; // refresh token TTL 7 ngày (<= 30 ngày)

interface MockRefreshToken {
  tokenHash: string;
  userId: number;
  expiresAt: number;
  revokedAt: number | null;
  replacedBy: string | null;
}

// ponytail: chain lưu localStorage (chỉ MOCK data — hash, không phải token thật;
// cần persist qua reload vì Map in-memory mất khi F5, cookie thì sống).
const RT_STORE_KEY = 'mockRefreshTokens';
const loadRefreshTokens = (): Map<string, MockRefreshToken> => {
  try {
    const raw = localStorage.getItem(RT_STORE_KEY);
    const list = raw ? (JSON.parse(raw) as MockRefreshToken[]) : [];
    return new Map(list.map((t) => [t.tokenHash, t]));
  } catch {
    return new Map();
  }
};
const saveRefreshTokens = (map: Map<string, MockRefreshToken>) => {
  try {
    localStorage.setItem(RT_STORE_KEY, JSON.stringify([...map.values()]));
  } catch {
    /* mock only */
  }
};

const refreshTokens = loadRefreshTokens();

// ponytail: hash djb2 — chỉ dành cho mock; BE thật hash server-side
const hashToken = (raw: string): string => {
  let h = 5381;
  for (let i = 0; i < raw.length; i++) h = ((h << 5) + h + raw.charCodeAt(i)) | 0;
  return h.toString(36);
};

const issueRefreshToken = (userId: number): string => {
  const raw = crypto.randomUUID();
  const tokenHash = hashToken(raw);
  refreshTokens.set(tokenHash, {
    tokenHash,
    userId,
    expiresAt: Date.now() + RT_TTL_MS,
    revokedAt: null,
    replacedBy: null,
  });
  saveRefreshTokens(refreshTokens);
  document.cookie = `${RT_COOKIE}=${raw}; path=${RT_PATH}; SameSite=Strict; Max-Age=${7 * 86400}`;
  return raw;
};

const readRefreshToken = (): MockRefreshToken | null => {
  const raw = document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${RT_COOKIE}=`))
    ?.slice(RT_COOKIE.length + 1);
  if (!raw) return null;
  return refreshTokens.get(hashToken(raw)) ?? null;
};

const revokeRefreshChain = (entry: MockRefreshToken | null) => {
  let current = entry;
  while (current) {
    current.revokedAt = Date.now();
    current = current.replacedBy
      ? refreshTokens.get(current.replacedBy) ?? null
      : null;
  }
  saveRefreshTokens(refreshTokens);
};

const clearRefreshCookie = () => {
  document.cookie = `${RT_COOKIE}=; path=${RT_PATH}; Max-Age=0`;
};

function ok<T>(data: T) {
  return of(
    new HttpResponse({
      status: 200,
      body: { code: 'OK', message: 'success', data },
    }),
  ).pipe(delay(150));
}

function fail(message: string, status = 400) {
  return throwError(
    () =>
      new HttpErrorResponse({
        status,
        statusText: message,
        error: { code: 'ERROR', message },
      }),
  );
}

function paginate(data: unknown[], page = 0, size = 100) {
  const totalElements = data.length;
  const start = page * size;
  const content = size > 0 ? data.slice(start, start + size) : data;
  return ok({
    content,
    pagination: {
      pageNumber: page,
      pageSize: size,
      totalElements,
    },
  });
}

const num = (v: string | null | undefined, fallback = 0) => {
  const n = Number(v);
  return Number.isNaN(n) ? fallback : n;
};

export function mockInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  const { method, url } = req;
  const params = req.params;
  const page = num(params.get('page'), 0);
  const size = num(params.get('size'), 100);

  if (!url.includes('/api/v1') && !url.includes('api.themoviedb.org')) {
    return next(req);
  }

  /* ---------------------------- AUTH ---------------------------- */
  if (url.includes('/auth/login') && method === 'POST') {
    const { identifier, password } = req.body as MockBody;
    const user = findByCredential(String(identifier), String(password));
    if (!user) return fail('Sai email hoặc mật khẩu', 401);
    issueRefreshToken(user.userId);
    const jwt = {
      userId: user.userId,
      username: user.username,
      email: user.email,
      accessToken: mockJwt(user),
    };
    return ok(jwt);
  }

  if (url.includes('/auth/register') && method === 'POST') {
    const { username, email, password } = req.body as MockBody;
    addUser(String(username), String(email), String(password));
    return ok('Tài khoản đã được tạo thành công');
  }

  if (url.includes('/auth/verify-email') && method === 'GET') {
    return ok('Email verified');
  }

  if (url.includes('/auth/resend-verification') && method === 'POST') {
    return ok(null);
  }

  if (url.includes('/auth/logout') && method === 'POST') {
    revokeRefreshChain(readRefreshToken());
    clearRefreshCookie();
    return ok(null);
  }

  if (url.includes('/auth/refresh-token') && method === 'POST') {
    const entry = readRefreshToken();
    if (!entry || entry.revokedAt !== null) {
      // replay/revoked → revoke toàn bộ chain rồi từ chối
      revokeRefreshChain(entry);
      clearRefreshCookie();
      return fail('Refresh token not found', 401);
    }
    if (entry.expiresAt < Date.now()) {
      return fail('Refresh token expired', 401);
    }
    const user = MOCK_USERS.find((u) => u.userId === entry.userId);
    if (!user) return fail('Refresh token invalid', 401);
    const nextRaw = issueRefreshToken(entry.userId);
    entry.revokedAt = Date.now();
    entry.replacedBy = hashToken(nextRaw);
    return ok({ accessToken: mockJwt(user) });
  }

  /* ---------------------------- MOVIE ---------------------------- */
  if (url.match(/\/movie\/\d+$/) && method === 'GET') {
    const id = num(url.split('/').pop());
    return ok(MOCK_MOVIES.find((m) => m.id === id) || null);
  }

  if (url.includes('/movie') && method === 'POST') {
    const movie = { ...(req.body as MockBody), id: MOCK_MOVIES.length + 1 };
    pushMock(MOCK_MOVIES, movie);
    return ok(movie);
  }

  if (url.match(/\/movie\/\d+$/) && method === 'PUT') {
    const id = num(url.split('/').pop());
    const idx = MOCK_MOVIES.findIndex((m) => m.id === id);
    if (idx === -1) return fail('Không tìm thấy phim', 404);
    MOCK_MOVIES[idx] = { ...MOCK_MOVIES[idx], ...(req.body as MockBody), id };
    return ok(MOCK_MOVIES[idx]);
  }

  if (url.match(/\/movie\/\d+$/) && method === 'DELETE') {
    const id = num(url.split('/').pop());
    const idx = MOCK_MOVIES.findIndex((m) => m.id === id);
    if (idx !== -1) MOCK_MOVIES.splice(idx, 1);
    return ok(null);
  }

  if (url.includes('/movie') && method === 'GET') {
    const query = (params.get('query') || '').toLowerCase();
    const data = MOCK_MOVIES.filter(
      (m) =>
        !query ||
        m.title.toLowerCase().includes(query) ||
        m.originalTitle.toLowerCase().includes(query),
    );
    return paginate(data, page, size);
  }

  /* ---------------------------- THEATER ---------------------------- */
  if (url.includes('/theater/search') && method === 'GET') {
    const location = (params.get('location') || '').toLowerCase();
    const data = MOCK_THEATERS.filter(
      (t) =>
        !location ||
        t.name.toLowerCase().includes(location) ||
        t.location.toLowerCase().includes(location),
    );
    return paginate(data);
  }

  if (url.match(/\/theater\/\d+$/) && method === 'GET') {
    const id = num(url.split('/').pop());
    return ok(MOCK_THEATERS.find((t) => t.id === id) || null);
  }

  if (url.match(/\/rooms\/theaters\/\d+$/) && method === 'GET') {
    const theaterId = num(url.match(/theaters\/(\d+)/)?.[1]);
    return ok([
      {
        id: theaterId * 10 + 1,
        theaterId,
        name: 'P1',
        roomType: '2D',
        totalSeats: 50,
        seatCount: 50,
      },
      {
        id: theaterId * 10 + 2,
        theaterId,
        name: 'P2',
        roomType: '3D',
        totalSeats: 60,
        seatCount: 60,
      },
    ]);
  }

  if (url.match(/\/rooms\/theaters\/\d+$/) && method === 'POST') {
    const theaterId = num(url.match(/theaters\/(\d+)/)?.[1]);
    return ok({ id: Date.now(), theaterId, ...(req.body as MockBody) });
  }

  if (url.match(/\/rooms\/\d+$/) && (method === 'PUT' || method === 'DELETE')) {
    return ok(null);
  }

  if (url.match(/\/rooms\/\d+\/seats$/) && method === 'GET') {
    const roomId = num(url.match(/rooms\/(\d+)/)?.[1]);
    const seats = [];
    for (const row of ['A', 'B', 'C']) {
      for (let i = 1; i <= 8; i++) {
        seats.push({
          id: roomId * 1000 + row.charCodeAt(0) * 100 + i,
          roomId,
          rowLabel: row,
          seatNumber: i,
          seatType: row === 'C' ? 'VIP' : 'NORMAL',
          posX: i,
          posY: row.charCodeAt(0) - 65,
        });
      }
    }
    return ok(seats);
  }

  if (url.match(/\/rooms\/\d+\/layout$/) && method === 'PUT') {
    return ok(
      ((req.body as unknown[]) ?? []).map((s, i) => ({
        ...(s as MockBody),
        id: Date.now() + i,
      })),
    );
  }

  if (url.includes('/theater') && method === 'POST') {
    const theater = {
      showtime: [],
      createdAt: new Date(),
      updatedAt: new Date(),
      isDeleted: false,
      ...(req.body as MockBody),
      id: MOCK_THEATERS.length + 1,
    };
    pushMock(MOCK_THEATERS, theater);
    return ok(theater);
  }

  if (url.match(/\/theater\/\d+$/) && method === 'PUT') {
    const id = num(url.split('/').pop());
    const idx = MOCK_THEATERS.findIndex((t) => t.id === id);
    if (idx === -1) return fail('Không tìm thấy rạp', 404);
    MOCK_THEATERS[idx] = { ...MOCK_THEATERS[idx], ...(req.body as MockBody), id };
    return ok(MOCK_THEATERS[idx]);
  }

  if (url.match(/\/theater\/\d+$/) && method === 'DELETE') {
    const id = num(url.split('/').pop());
    const idx = MOCK_THEATERS.findIndex((t) => t.id === id);
    if (idx !== -1) MOCK_THEATERS.splice(idx, 1);
    return ok(null);
  }

  if (url.match(/\/theater(\?|$)/) && method === 'GET') {
    return paginate(MOCK_THEATERS, page, size);
  }

  /* ---------------------------- SHOWTIME ---------------------------- */
  if (url.match(/\/showtime\/available\/movies\/\d+$/) && method === 'GET') {
    const movieId = num(url.split('/').pop());
    const all = [1, 2, 3, 4].flatMap((tid) => getShowtimes(tid));
    return ok(all.filter((st) => st.movieId === movieId));
  }

  if (url.match(/\/showtime\/theaters\/\d+$/) && method === 'GET') {
    const theaterId = num(url.match(/theaters\/(\d+)/)?.[1]);
    return paginate(getShowtimes(theaterId), page, size);
  }

  if (url.includes('/showtime/available') && method === 'GET') {
    return ok(getShowtimes(1));
  }

  if (url.match(/\/showtime\/movies\/\d+$/) && method === 'GET') {
    const movieId = num(url.match(/movies\/(\d+)/)?.[1]);
    const all = [1, 2, 3, 4].flatMap((tid) => getShowtimes(tid));
    return paginate(all.filter((st) => st.movieId === movieId), page, size);
  }

  if (url.match(/\/showtime\/\d+$/) && method === 'GET') {
    const id = num(url.split('/').pop());
    const all = [1, 2, 3, 4].flatMap((tid) => getShowtimes(tid));
    return ok(all.find((st) => st.id === id) || null);
  }

  if (url.includes('/showtime') && method === 'POST') {
    const all = [1, 2, 3, 4].flatMap((tid) => getShowtimes(tid));
    const { showDate, showTime } = req.body as MockBody;
    const showtime = {
      id: Math.max(...all.map((s) => s.id)) + 1,
      ...(req.body as MockBody),
      showDate: showDate
        ? new Date(String(showDate).split('/').reverse().join('-'))
        : showDate,
      showTime: showTime
        ? new Date(`1970-01-01T${showTime}`)
        : showTime,
      createdAt: new Date(),
      updatedAt: new Date(),
      isDeleted: false,
    };
    return ok(showtime);
  }

  if (url.match(/\/showtime\/\d+$/) && method === 'PUT') {
    const id = num(url.split('/').pop());
    const { showDate, showTime } = req.body as MockBody;
    return ok({
      id,
      ...(req.body as MockBody),
      showDate: showDate
        ? new Date(String(showDate).split('/').reverse().join('-'))
        : showDate,
      showTime: showTime
        ? new Date(`1970-01-01T${showTime}`)
        : showTime,
    });
  }

  if (url.match(/\/showtime\/\d+$/) && method === 'DELETE') {
    return ok(null);
  }

  if (url.includes('/showtime') && method === 'GET') {
    return ok(getShowtimes(1));
  }

  /* ---------------------------- SEAT ---------------------------- */
  if (url.match(/\/seats\/showtime\/\d+\/available$/) && method === 'GET') {
    const showtimeId = num(url.match(/showtime\/(\d+)/)?.[1]);
    return ok(getSeats(showtimeId));
  }

  if (url.match(/\/seats\/showtime\/\d+$/) && method === 'GET') {
    const showtimeId = num(url.match(/showtime\/(\d+)/)?.[1]);
    return ok(getSeats(showtimeId));
  }

  /* ---------------------------- RESERVATION ---------------------------- */
  if (url.includes('/reservation/init') && method === 'POST') {
    const { showtimeId } = req.body as MockBody;
    const reservationId = `res-${Math.floor(1000 + Math.random() * 9000)}`;
    return ok({
      reservationCode: reservationId,
      reservationId,
      showtimeId,
      ttl: 600,
      expiredAt: new Date(Date.now() + 600000),
    });
  }

  if (url.includes('/reservation/seatHold') && method === 'POST') {
    return ok(null);
  }

  if (url.match(/\/reservation\/.+?\/voucher/) && method === 'POST') {
    const code = params.get('code') || '';
    return ok({
      code,
      originalAmount: 0,
      discountAmount: 0,
      finalAmount: 0,
    });
  }

  if (url.includes('/reservation/me') && method === 'GET') {
    const userId = num(params.get('userId'));
    const data = MOCK_RESERVATIONS.filter(
      (r) => r.user.userId === userId || userId === 0,
    ).map(toReservationProfile);
    return paginate(data);
  }

  if (url.match(/\/reservation\/.+?\/restore/) && method === 'GET') {
    return ok({
      reservationCode: 'res-002',
      reservationId: 'res-002',
      showtimeId: 101,
      ttl: 600,
      expiredAt: new Date(Date.now() + 600000),
    });
  }

  if (url.match(/\/reservation\/[^/]+$/) && method === 'GET') {
    const id = url.split('/').pop()!;
    const reservation =
      MOCK_RESERVATIONS.find((r) => r.id === id) || MOCK_RESERVATIONS[0];
    return ok(reservation);
  }

  if (url.includes('/reservation') && method === 'GET') {
    const status = params.get('reservationStatus');
    const userId = num(params.get('userId'));
    const data = MOCK_RESERVATIONS.filter(
      (r) =>
        (!status || r.reservationStatus === status) &&
        (userId === 0 || r.user.userId === userId),
    );
    return paginate(data, page, size);
  }

  /* ---------------------------- PAYMENT ---------------------------- */
  if (url.includes('/payment/create') && method === 'GET') {
    const reservationId = params.get('reservationId') || 'res-001';
    const success = Math.random() > 0.25;
    return ok({
      code: 'OK',
      message: 'success',
      paymentUrl: `/paymentResult?status=${success ? 'success' : 'error'}&reservationId=${reservationId}`,
    });
  }

  /* ---------------------------- USER ---------------------------- */
  if (url.match(/\/user\/update\/\d+\/profile$/) && method === 'PUT') {
    const id = num(url.match(/update\/(\d+)/)?.[1]);
    const user = publicUsers().find((u) => u.userId === id);
    return ok({ ...user, ...(req.body as MockBody), userId: id });
  }

  if (url.match(/\/user\/update\/\d+\/status$/) && method === 'PUT') {
    const id = num(url.match(/update\/(\d+)/)?.[1]);
    const user = publicUsers().find((u) => u.userId === id);
    const isDeleted = (req.body as MockBody)?.['isDeleted'];
    return ok({ ...user, isDeleted });
  }

  if (url.includes('/user/email/') && method === 'GET') {
    const email = decodeURIComponent(url.split('/email/')[1]);
    const user = publicUsers().find((u) => u.email === email);
    return ok(user || null);
  }

  if (url.match(/\/user\/\d+$/) && method === 'GET') {
    const id = num(url.split('/').pop());
    return ok(publicUsers().find((u) => u.userId === id) || null);
  }

  if (url.includes('/user') && method === 'GET') {
    return paginate(publicUsers(), page, size);
  }

  /* ---------------------------- ARTICLE ---------------------------- */
  if (url.match(/\/article\/\d+$/) && method === 'GET') {
    const id = num(url.split('/').pop());
    return ok(MOCK_ARTICLES.find((a) => a.id === id) || null);
  }

  if (url.includes('/article') && method === 'POST') {
    const article = { ...(req.body as MockBody), id: MOCK_ARTICLES.length + 1 };
    pushMock(MOCK_ARTICLES, article);
    return ok(article);
  }

  if (url.match(/\/article\/\d+$/) && method === 'PUT') {
    const id = num(url.split('/').pop());
    const idx = MOCK_ARTICLES.findIndex((a) => a.id === id);
    if (idx === -1) return fail('Không tìm thấy bài viết', 404);
    MOCK_ARTICLES[idx] = { ...MOCK_ARTICLES[idx], ...(req.body as MockBody), id };
    return ok(MOCK_ARTICLES[idx]);
  }

  if (url.match(/\/article\/\d+$/) && method === 'DELETE') {
    const id = num(url.split('/').pop());
    const idx = MOCK_ARTICLES.findIndex((a) => a.id === id);
    if (idx !== -1) MOCK_ARTICLES.splice(idx, 1);
    return ok(null);
  }

  if (url.includes('/article') && method === 'GET') {
    const query = (params.get('query') || '').toLowerCase();
    const data = MOCK_ARTICLES.filter(
      (a) => !query || a.title.toLowerCase().includes(query),
    );
    return paginate(data, page, size);
  }

  /* ---------------------------- VOUCHER ---------------------------- */
  if (url.includes('/voucher/calculate') && method === 'GET') {
    const code = params.get('code') || '';
    const total = num(params.get('total'));
    const found = VOUCHER_CALCULATIONS[code] as {
      code: string;
      discountAmount: number;
    } | null;
    if (!found) return fail('Mã giảm giá không hợp lệ', 400);
    const discount = Math.min(found.discountAmount, total);
    return ok({
      code: found.code,
      originalAmount: total,
      discountAmount: discount,
      finalAmount: total - discount,
    });
  }

  if (url.includes('/voucher/user') && method === 'GET') {
    return ok([
      {
        id: 1,
        code: 'WELCOME10',
        voucherName: 'Chào mừng thành viên mới',
        discountType: 'FIXED',
        discountValue: 10000,
        maxDiscountAmount: 10000,
        minOrderValue: 100000,
        status: 'AVAILABLE',
        expiredAt: new Date(Date.now() + 30 * 86400000),
      },
    ]);
  }

  if (url.includes('/voucher/claim') && method === 'POST') {
    const code = params.get('code') || 'WELCOME10';
    return ok({
      id: Date.now(),
      code,
      voucherName: code,
      discountType: 'FIXED',
      discountValue: 10000,
      maxDiscountAmount: 10000,
      minOrderValue: 0,
      status: 'AVAILABLE',
      expiredAt: new Date(Date.now() + 30 * 86400000),
    });
  }

  /* ---------------------------- AUDIT ---------------------------- */
  if (url.includes('/audit-logs') && method === 'GET') {
    return paginate([]);
  }

  if (url.match(/\/voucher\/\d+$/) && method === 'GET') {
    const id = num(url.split('/').pop());
    return ok(MOCK_VOUCHERS.find((v) => v.id === id) || null);
  }

  if (url.includes('/voucher') && method === 'POST') {
    const voucher = { id: MOCK_VOUCHERS.length + 1, ...(req.body as MockBody) };
    pushMock(MOCK_VOUCHERS, voucher);
    return ok(voucher);
  }

  if (url.match(/\/voucher\/\d+$/) && method === 'PUT') {
    const id = num(url.split('/').pop());
    const idx = MOCK_VOUCHERS.findIndex((v) => v.id === id);
    if (idx === -1) return fail('Không tìm thấy voucher', 404);
    MOCK_VOUCHERS[idx] = { ...MOCK_VOUCHERS[idx], ...(req.body as MockBody), id };
    return ok(MOCK_VOUCHERS[idx]);
  }

  if (url.match(/\/voucher\/\d+$/) && method === 'DELETE') {
    const id = num(url.split('/').pop());
    const idx = MOCK_VOUCHERS.findIndex((v) => v.id === id);
    if (idx !== -1) MOCK_VOUCHERS.splice(idx, 1);
    return ok(null);
  }

  if (url.includes('/voucher') && method === 'GET') {
    const query = (params.get('query') || '').toLowerCase();
    const data = MOCK_VOUCHERS.filter(
      (v) => !query || v.name.toLowerCase().includes(query),
    );
    return paginate(data, page, size);
  }

  /* ---------------------------- DASHBOARD ---------------------------- */
  if (url.includes('/dashboard/summary') && method === 'GET') {
    return ok(MOCK_DASHBOARD_SUMMARY);
  }

  /* ---------------------------- AI / CLOUDINARY ---------------------------- */
  if (url.includes('/ai/chat') && method === 'POST') {
    return ok('Xin chào! Tôi là trợ lý ảo của CinePlex. Tôi có thể giúp gì cho bạn?');
  }

  if (url.includes('/cloudinary/upload') && method === 'POST') {
    return ok({ secure_url: 'https://placehold.co/300x400/e4002b/fff?text=CinePlex' });
  }

  if (url.includes('/report/export/') && method === 'GET') {
    return of(
      new HttpResponse({
        status: 200,
        body: new Blob(['mock,csv,data'], { type: 'text/csv' }),
      }),
    );
  }

  /* ---------------------------- TMDB ---------------------------- */
  if (url.includes('/search/movie') && method === 'GET') {
    const query = url.split('query=')[1]?.split('&')[0] || '';
    const results = MOCK_MOVIES.filter(
      (m) =>
        m.title.toLowerCase().includes(query.toLowerCase()) ||
        m.originalTitle.toLowerCase().includes(query.toLowerCase()),
    ).map((m) => ({
      id: m.id,
      title: m.title,
      original_title: m.originalTitle,
      overview: m.overview,
      release_date: m.releaseDate,
      poster_path: m.poster.replace('https://image.tmdb.org/t/p/w500', ''),
      backdrop_path: m.backdrop.replace('https://image.tmdb.org/t/p/w1280', ''),
    }));
    return ok({ results });
  }

  if (url.includes('/movie/') && url.includes('themoviedb.org') && method === 'GET') {
    const id = num(url.split('/movie/')[1]?.split('?')[0]);
    const m = MOCK_MOVIES.find((x) => x.id === id);
    if (!m) return fail('Not found', 404);
    return ok({
      id: m.id,
      title: m.title,
      original_title: m.originalTitle,
      overview: m.overview,
      release_date: m.releaseDate,
      runtime: m.duration,
      genres: m.genres.map((g) => ({ id: g.length, name: g })),
      poster_path: m.poster.replace('https://image.tmdb.org/t/p/w500', ''),
      backdrop_path: m.backdrop.replace('https://image.tmdb.org/t/p/w1280', ''),
    });
  }

  return next(req);
}

void delay;
void MOCK_USERS;
