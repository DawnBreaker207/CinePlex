import { ReservationProfile } from '@domain/reservation/models/reservation.model';
import { SeatStatus } from '@core/constants/enum';

const now = () => new Date();
const ago = (days: number) => new Date(Date.now() - days * 86400000);
const ahead = (days: number) => new Date(Date.now() + days * 86400000);

const avatar = (name: string) =>
  `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=e4002b&color=fff&bold=true`;

export interface MockUser {
  userId: number;
  username: string;
  avatar: string;
  email: string;
  phone: string;
  address: string;
  role: string;
  password: string;
}

export const MOCK_USERS: MockUser[] = [
  {
    userId: 1,
    username: 'admin',
    avatar: avatar('admin'),
    email: 'admin@cineplex.vn',
    phone: '0901234567',
    address: '72 Lê Thánh Tôn, Quận 1, TP.HCM',
    role: 'ADMIN',
    password: 'admin',
  },
  {
    userId: 2,
    username: 'Moderator',
    avatar: avatar('Moderator'),
    email: 'mod@cineplex.vn',
    phone: '0912345678',
    address: '34 Lê Duẩn, Quận 1, TP.HCM',
    role: 'MODERATOR',
    password: '123456',
  },
  {
    userId: 3,
    username: 'Nguyễn Văn An',
    avatar: avatar('Nguyễn Văn An'),
    email: 'user@cineplex.vn',
    phone: '0923456789',
    address: '116 Nguyễn Du, Quận 1, TP.HCM',
    role: 'USER',
    password: '123456',
  },
  {
    userId: 4,
    username: 'Trần Thị Bình',
    avatar: avatar('Trần Thị Bình'),
    email: 'binh@cineplex.vn',
    phone: '0934567890',
    address: '88 Lê Lai, Quận 1, TP.HCM',
    role: 'USER',
    password: '123456',
  },
];

export const publicUsers = () =>
  MOCK_USERS.map(({ password: _password, ...user }) => user);

export const findByCredential = (identifier: string, password: string) =>
  MOCK_USERS.find(
    (u) =>
      (u.email === identifier || u.username === identifier) &&
      u.password === password,
  );

export const addUser = (username: string, email: string, password: string) => {
  const user: MockUser = {
    userId: Math.max(...MOCK_USERS.map((u) => u.userId)) + 1,
    username,
    avatar: avatar(username),
    email,
    phone: '',
    address: '',
    role: 'USER',
    password,
  };
  MOCK_USERS.push(user);
  return user;
};

export const MOCK_THEATERS = [
  {
    id: 1,
    name: 'CGV Vincom Center',
    location: 'Vincom Center, 72 Lê Thánh Tôn, Q.1',
    capacity: 120,
    createdAt: ago(30),
    updatedAt: ago(5),
    isDeleted: false,
  },
  {
    id: 2,
    name: 'Lotte Cinema Diamond',
    location: 'Diamond Plaza, 34 Lê Duẩn, Q.1',
    capacity: 140,
    createdAt: ago(25),
    updatedAt: ago(4),
    isDeleted: false,
  },
  {
    id: 3,
    name: 'Galaxy Nguyễn Du',
    location: '116 Nguyễn Du, Q.1',
    capacity: 100,
    createdAt: ago(20),
    updatedAt: ago(3),
    isDeleted: false,
  },
  {
    id: 4,
    name: 'BHD Star Bitexco',
    location: 'Tầng 2, Bitexco Tower, Q.1',
    capacity: 160,
    createdAt: ago(15),
    updatedAt: ago(2),
    isDeleted: false,
  },
];

export const MOCK_MOVIES = [
  {
    id: 1,
    title: 'Kẻ Trộm Mặt Trăng 4',
    originalTitle: 'Despicable Me 4',
    poster: 'https://image.tmdb.org/t/p/w500/l3zC6FSDz2TBcZ3FUlOqmlCT6Oc.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/A6Q1p3U0CM4JME9KqzHI3SUJ9oU.jpg',
    overview:
      'Gru, Lucy, và các Minion bước vào kỷ nguyên mới đầy hỗn loạn khi gia đình mở rộng.',
    duration: 95,
    releaseDate: '2026-07-15',
    language: 'Tiếng Anh',
    country: 'Mỹ',
    genres: ['Hoạt hình', 'Hài hước', 'Phiêu lưu'],
    imdbId: 'tt7510222',
    filmId: 'D4-2026',
    createdAt: ago(40),
    updatedAt: ago(2),
    isDeleted: false,
  },
  {
    id: 2,
    title: 'Deadpool & Wolverine',
    originalTitle: 'Deadpool & Wolverine',
    poster: 'https://image.tmdb.org/t/p/w500/wTnO3R4iCButT9xMpEoJmLEBX6R.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/yDHYYNBgBZPKhouwQ5oSH4JL3pW.jpg',
    overview:
      'Deadpool gia nhập Vũ trụ Điện ảnh Marvel cùng với Wolverine.',
    duration: 128,
    releaseDate: '2026-07-26',
    language: 'Tiếng Anh',
    country: 'Mỹ',
    genres: ['Hành động', 'Hài hước', 'Siêu anh hùng'],
    imdbId: 'tt6263850',
    filmId: 'DW-2026',
    createdAt: ago(35),
    updatedAt: ago(1),
    isDeleted: false,
  },
  {
    id: 3,
    title: 'Inside Out 2',
    originalTitle: 'Inside Out 2',
    poster: 'https://image.tmdb.org/t/p/w500/4Kxq8PToUVc4wV8UwSCOcMrS4Tq.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/5YF6oEfPGR5GJbkYp4TYiF9Lp3U.jpg',
    overview: 'Riley bước vào tuổi dậy thì với những cảm xúc mới.',
    duration: 102,
    releaseDate: '2026-06-14',
    language: 'Tiếng Anh',
    country: 'Mỹ',
    genres: ['Hoạt hình', 'Tâm lý', 'Gia đình'],
    imdbId: 'tt22022452',
    filmId: 'IO2-2026',
    createdAt: ago(32),
    updatedAt: ago(1),
    isDeleted: false,
  },
  {
    id: 4,
    title: 'Twisters',
    originalTitle: 'Twisters',
    poster: 'https://image.tmdb.org/t/p/w500/7nGf6YVcs3rA7nTfBisT6VfGzXs.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/lF1pBAFxnQ2JrMD3IkIln1IFMJs.jpg',
    overview: 'Những thợ săn bão đối mặt với lốc xoáy siêu cường.',
    duration: 117,
    releaseDate: '2026-07-19',
    language: 'Tiếng Anh',
    country: 'Mỹ',
    genres: ['Hành động', 'Phiêu lưu'],
    imdbId: 'tt12584954',
    filmId: 'TW-2026',
    createdAt: ago(28),
    updatedAt: ago(1),
    isDeleted: false,
  },
  {
    id: 5,
    title: 'A Quiet Place: Day One',
    originalTitle: 'A Quiet Place: Day One',
    poster: 'https://image.tmdb.org/t/p/w500/5YF6oEfPGR5GJbkYp4TYiF9Lp3U.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/2xLk2HJXU0CzVJwKkEEgvn82X9K.jpg',
    overview:
      'Ngày đầu tiên khi sinh vật ngoài hành tinh tấn công New York.',
    duration: 100,
    releaseDate: '2026-08-25',
    language: 'Tiếng Anh',
    country: 'Mỹ',
    genres: ['Kinh dị', 'Viễn tưởng'],
    imdbId: 'tt13433802',
    filmId: 'AQP-2026',
    createdAt: ago(25),
    updatedAt: ago(1),
    isDeleted: false,
  },
  {
    id: 6,
    title: 'Mưa Đỏ',
    originalTitle: 'Mua Do',
    poster: 'https://image.tmdb.org/t/p/w500/5YF6oEfPGR5GJbkYp4TYiF9Lp3U.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/2xLk2HJXU0CzVJwKkEEgvn82X9K.jpg',
    overview:
      'Câu chuyện cảm động về tình đồng đội trong chiến dịch lịch sử.',
    duration: 125,
    releaseDate: '2026-10-02',
    language: 'Tiếng Việt',
    country: 'Việt Nam',
    genres: ['Chiến tranh', 'Tâm lý'],
    imdbId: 'tt30000001',
    filmId: 'MD-2026',
    createdAt: ago(5),
    updatedAt: ago(1),
    isDeleted: false,
  },
  {
    id: 7,
    title: 'Siêu Lừa Gặp Siêu Lầy',
    originalTitle: 'Sieu Lua Gap Sieu Lay',
    poster: 'https://image.tmdb.org/t/p/w500/7nGf6YVcs3rA7nTfBisT6VfGzXs.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/lF1pBAFxnQ2JrMD3IkIln1IFMJs.jpg',
    overview: 'Phi vụ lừa đảo dở khóc dở cười của bộ đôi hoàn cảnh.',
    duration: 110,
    releaseDate: '2026-10-16',
    language: 'Tiếng Việt',
    country: 'Việt Nam',
    genres: ['Hài hước'],
    imdbId: 'tt30000002',
    filmId: 'SL-2026',
    createdAt: ago(3),
    updatedAt: ago(1),
    isDeleted: false,
  },
];

function generateSeats(showtimeId: number) {
  const rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
  const seats = [];
  let id = 1;
  for (const row of rows) {
    for (let col = 1; col <= 10; col++) {
      seats.push({
        id: id + showtimeId * 1000,
        showtimeId,
        seatNumber: `${row}${col}`,
        status: 'AVAILABLE' as SeatStatus,
      });
      id++;
    }
  }
  seats[3].status = 'BOOKED';
  seats[7].status = 'BOOKED';
  seats[12].status = 'HOLD';
  seats[15].status = 'HOLD';
  seats[24].status = 'BOOKED';
  seats[28].status = 'BOOKED';
  seats[31].status = 'BOOKED';
  return seats;
}

export function getSeats(showtimeId: number) {
  return generateSeats(showtimeId);
}

export function getShowtimes(theaterId: number) {
  const baseTimes = ['08:30', '10:30', '13:00', '14:30', '16:45', '19:00', '21:30'];
  const theater = MOCK_THEATERS.find((t) => t.id === theaterId) ?? MOCK_THEATERS[0];
  const showtimes = [];
  let id = theaterId * 100;
  for (const movie of MOCK_MOVIES) {
    for (let day = 0; day < 3; day++) {
      const date = ahead(day);
      const slots = baseTimes.slice(0, 3 + day);
      for (const time of slots) {
        id++;
        const [h, m] = time.split(':').map(Number);
        const showTime = new Date(date);
        showTime.setHours(h, m, 0, 0);
        showtimes.push({
          id,
          movieId: movie.id,
          movieTitle: movie.title,
          moviePosterUrl: movie.poster,
          theaterId: theater.id,
          theaterName: theater.name,
          theaterLocation: theater.location,
          showDate: new Date(date),
          showTime,
          price: 65000 + day * 5000 + (id % 3) * 10000,
          totalSeats: 80,
          availableSeats: 20 + (id % 60),
          createdAt: now(),
          updatedAt: now(),
          isDeleted: false,
        });
      }
    }
  }
  return showtimes;
}

MOCK_THEATERS.forEach((t) => {
  Object.assign(t, { showtime: getShowtimes(t.id) });
});

export const MOCK_SEATS = generateSeats(101);

const showtimeById = (id: number) =>
  [1, 2, 3, 4].flatMap((tid) => getShowtimes(tid)).find((s) => s.id === id);

export const MOCK_RESERVATIONS = [
  {
    id: 'res-001',
    user: publicUsers()[2],
    showtime: showtimeById(101) ?? getShowtimes(1)[0],
    reservationStatus: 'CONFIRMED',
    totalAmount: 130000,
    seats: [
      { id: 1, showtimeId: 101, seatNumber: 'D4', status: 'BOOKED' },
      { id: 2, showtimeId: 101, seatNumber: 'D5', status: 'BOOKED' },
    ],
    createdAt: ago(2),
    updatedAt: ago(2),
    isDeleted: false,
  },
  {
    id: 'res-002',
    user: publicUsers()[2],
    showtime: showtimeById(102) ?? getShowtimes(1)[0],
    reservationStatus: 'PENDING',
    totalAmount: 70000,
    seats: [
      { id: 3, showtimeId: 102, seatNumber: 'B1', status: 'HOLD' },
    ],
    createdAt: ago(1),
    updatedAt: ago(1),
    isDeleted: false,
  },
  {
    id: 'res-003',
    user: publicUsers()[3],
    showtime: showtimeById(201) ?? getShowtimes(2)[0],
    reservationStatus: 'CANCELED',
    totalAmount: 195000,
    seats: [
      { id: 4, showtimeId: 201, seatNumber: 'E2', status: 'AVAILABLE' },
      { id: 5, showtimeId: 201, seatNumber: 'E3', status: 'AVAILABLE' },
    ],
    createdAt: ago(3),
    updatedAt: ago(2),
    isDeleted: false,
  },
  {
    id: 'res-004',
    user: publicUsers()[3],
    showtime: showtimeById(202) ?? getShowtimes(2)[0],
    reservationStatus: 'REFUNDED',
    totalAmount: 65000,
    seats: [
      { id: 6, showtimeId: 202, seatNumber: 'F5', status: 'AVAILABLE' },
    ],
    createdAt: ago(5),
    updatedAt: ago(4),
    isDeleted: false,
  },
];

export const toReservationProfile = (r: (typeof MOCK_RESERVATIONS)[0]): ReservationProfile => ({
  reservationId: r.id,
  moviePoster: r.showtime.moviePosterUrl,
  movieTitle: r.showtime.movieTitle,
  room: r.showtime.theaterName,
  date: r.showtime.showDate.toISOString(),
  time: r.showtime.showTime.toISOString(),
  theater: r.showtime.theaterName,
  seats: r.seats.map((s) => s.seatNumber),
  amount: r.totalAmount,
});

export const MOCK_ARTICLES = [
  {
    id: 1,
    title: 'Ưu đãi 20% cho thành viên mới',
    summary: 'Nhận ngay ưu đãi khi đăng ký tài khoản CinePlex.',
    thumbnail: 'https://image.tmdb.org/t/p/w500/l3zC6FSDz2TBcZ3FUlOqmlCT6Oc.jpg',
    content: 'Đăng ký tài khoản CinePlex ngay hôm nay để nhận mã ưu đãi CINEPLEX20.',
    author: 'Admin',
    status: 'PUBLISHED',
    type: 'PROMOTION',
    category: 'Khuyến mãi',
    views: 1200,
    createdAt: ago(7),
  },
  {
    id: 2,
    title: 'Phim hay tháng 7: Kẻ Trộm Mặt Trăng 4',
    summary: 'Điểm danh những bộ phim đáng xem nhất tháng 7.',
    thumbnail: 'https://image.tmdb.org/t/p/w500/wTnO3R4iCButT9xMpEoJmLEBX6R.jpg',
    content: 'Tháng 7 chứng kiến sự trở lại của loạt phim hoạt hình đình đám.',
    author: 'Moderator',
    status: 'PUBLISHED',
    type: 'NEWS',
    category: 'Tin tức',
    views: 3400,
    createdAt: ago(3),
  },
  {
    id: 3,
    title: 'Combo bắp nước tiết kiệm',
    summary: 'Combo 2 bắp 2 nước chỉ từ 89.000đ.',
    thumbnail: 'https://image.tmdb.org/t/p/w500/4Kxq8PToUVc4wV8UwSCOcMrS4Tq.jpg',
    content: 'Chương trình combo đồ ăn áp dụng tại tất cả cụm rạp CinePlex.',
    author: 'Admin',
    status: 'DRAFT',
    type: 'PROMOTION',
    category: 'Khuyến mãi',
    views: 80,
    createdAt: ago(1),
  },
  {
    id: 4,
    title: 'Bí mật hậu trường Inside Out 2',
    summary: 'Pixar hé lộ quá trình sản xuất bom tấn hoạt hình.',
    thumbnail: 'https://image.tmdb.org/t/p/w500/7nGf6YVcs3rA7nTfBisT6VfGzXs.jpg',
    content: 'Đội ngũ Pixar chia sẻ về kỹ thuật hoạt hình mới.',
    author: 'Moderator',
    status: 'PUBLISHED',
    type: 'NEWS',
    category: 'Tin tức',
    views: 2100,
    createdAt: ago(6),
  },
  {
    id: 5,
    title: 'Twisters: Sức hút của thể loại thảm họa',
    summary: 'Vì sao khán giả vẫn mê phim thảm họa thiên nhiên?',
    thumbnail: 'https://image.tmdb.org/t/p/w500/5YF6oEfPGR5GJbkYp4TYiF9Lp3U.jpg',
    content: 'Phân tích yếu tố khiến Twisters trở thành hiện tượng phòng vé.',
    author: 'Admin',
    status: 'DRAFT',
    type: 'NEWS',
    category: 'Tin tức',
    views: 320,
    createdAt: ago(2),
  },
];

export const MOCK_VOUCHERS = [
  {
    id: 1,
    name: 'Chào mừng thành viên mới',
    code: 'WELCOME10',
    startAt: ahead(-30),
    endAt: ahead(60),
    quantityTotal: 1000,
    quantityUsed: 312,
    category: 'CAMPAIGN',
    groupRef: 'CAMPAIGN-2026-01',
    conditions: 'Áp dụng cho đơn từ 0đ',
    discountType: 'FIXED',
    discountValue: 10000,
    maxDiscountAmount: 10000,
    minOrderValue: 0,
    isActive: true,
    createdAt: ago(30),
    updatedAt: ago(1),
  },
  {
    id: 2,
    name: 'Giảm 20K cho đơn từ 50K',
    code: 'CINEPLEX20',
    startAt: ahead(-15),
    endAt: ahead(30),
    quantityTotal: 500,
    quantityUsed: 128,
    category: 'SYSTEM',
    groupRef: 'SYSTEM-2026-02',
    conditions: 'Áp dụng cho đơn từ 50.000đ',
    discountType: 'FIXED',
    discountValue: 20000,
    maxDiscountAmount: 20000,
    minOrderValue: 50000,
    isActive: true,
    createdAt: ago(15),
    updatedAt: ago(2),
  },
  {
    id: 3,
    name: 'Giảm 15% cho người dùng mới',
    code: 'NEWUSER',
    startAt: ahead(-5),
    endAt: ahead(90),
    quantityTotal: 2000,
    quantityUsed: 45,
    category: 'CAMPAIGN',
    groupRef: 'CAMPAIGN-2026-03',
    conditions: 'Giảm tối đa 30.000đ',
    discountType: 'PERCENT',
    discountValue: 15,
    maxDiscountAmount: 30000,
    minOrderValue: 0,
    isActive: true,
    createdAt: ago(5),
    updatedAt: ago(1),
  },
  {
    id: 4,
    name: 'Voucher cuối tuần',
    code: 'WEEKEND',
    startAt: ahead(-1),
    endAt: ahead(10),
    quantityTotal: 300,
    quantityUsed: 300,
    category: 'CAMPAIGN',
    groupRef: 'CAMPAIGN-2026-04',
    conditions: 'Hết hạn sử dụng',
    discountType: 'FIXED',
    discountValue: 5000,
    maxDiscountAmount: 5000,
    minOrderValue: 30000,
    isActive: false,
    createdAt: ago(1),
    updatedAt: ago(1),
  },
];

export const VOUCHER_CALCULATIONS: Record<string, unknown> = {
  WELCOME10: {
    code: 'WELCOME10',
    originalAmount: 0,
    discountAmount: 10000,
    finalAmount: 0,
  },
  CINEPLEX20: {
    code: 'CINEPLEX20',
    originalAmount: 0,
    discountAmount: 20000,
    finalAmount: 0,
  },
  NEWUSER: {
    code: 'NEWUSER',
    originalAmount: 0,
    discountAmount: 15000,
    finalAmount: 0,
  },
};

export const MOCK_DASHBOARD_SUMMARY = {
  metrics: {
    totalRevenue: 152750000,
    ticketsSold: 2340,
    activeTheaters: 4,
    seatUtilization: 67,
  },
  revenues: Array.from({ length: 12 }, (_, i) => ({
    date: new Date(2026, i, 1),
    revenue: 80000000 + (i % 4) * 10000000 + i * 2500000,
  })),
  movies: MOCK_MOVIES.slice(0, 5).map((m, i) => ({
    movieName: m.title,
    ticketSold: 500 - i * 90,
    revenue: 30000000 - i * 4000000,
  })),
  theaters: MOCK_THEATERS.map((t, i) => ({
    theaterName: t.name,
    ticketsSold: 400 - i * 70,
    totalRevenue: 25000000 - i * 3000000,
  })),
  payments: [
    { method: 'MOMO', amount: 45000000 },
    { method: 'VNPAY', amount: 40000000 },
    { method: 'ZALOPAY', amount: 30000000 },
    { method: 'CASH', amount: 37750000 },
  ],
};

export const MOCK_JWT_TOKEN =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoiYWRtaW5AY2luZXBsZXgudm4iLCJ1c2VybmFtZSI6ImFkbWluIiwicm9sZXMiOlsiQURNSU4iXSwiZXhwIjo5OTk5OTk5OTk5fQ.mocksig';

export const MOCK_RESERVATION_INIT = {
  reservationId: 'res-002',
  showtimeId: 101,
  ttl: 600,
  expiredAt: new Date(Date.now() + 600000),
};
