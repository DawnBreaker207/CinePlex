export const MOCK_THEATERS = [
  { id: 1, name: 'CGV Vincom Center', location: 'Vincom Center, 72 Lê Thánh Tôn, Q.1' },
  { id: 2, name: 'Lotte Cinema Diamond', location: 'Diamond Plaza, 34 Lê Duẩn, Q.1' },
  { id: 3, name: 'Galaxy Nguyễn Du', location: '116 Nguyễn Du, Q.1' },
];

export const MOCK_MOVIES = [
  {
    id: 1, title: 'Kẻ Trộm Mặt Trăng 4', originalTitle: 'Despicable Me 4',
    poster: 'https://image.tmdb.org/t/p/w500/l3zC6FSDz2TBcZ3FUlOqmlCT6Oc.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/A6Q1p3U0CM4JME9KqzHI3SUJ9oU.jpg',
    overview: 'Gru, Lucy, và các Minion bước vào kỷ nguyên mới đầy hỗn loạn khi gia đình mở rộng.',
    duration: 95, releaseDate: '2026-07-15', language: 'Tiếng Anh', country: 'Mỹ',
    genres: ['Hoạt hình', 'Hài hước', 'Phiêu lưu'],
  },
  {
    id: 2, title: 'Deadpool & Wolverine', originalTitle: 'Deadpool & Wolverine',
    poster: 'https://image.tmdb.org/t/p/w500/wTnO3R4iCButT9xMpEoJmLEBX6R.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/yDHYYNBgBZPKhouwQ5oSH4JL3pW.jpg',
    overview: 'Deadpool gia nhập Vũ trụ Điện ảnh Marvel cùng với Wolverine.',
    duration: 128, releaseDate: '2026-07-26', language: 'Tiếng Anh', country: 'Mỹ',
    genres: ['Hành động', 'Hài hước', 'Siêu anh hùng'],
  },
  {
    id: 3, title: 'Inside Out 2', originalTitle: 'Inside Out 2',
    poster: 'https://image.tmdb.org/t/p/w500/4Kxq8PToUVc4wV8UwSCOcMrS4Tq.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/5YF6oEfPGR5GJbkYp4TYiF9Lp3U.jpg',
    overview: 'Riley bước vào tuổi dậy thì với những cảm xúc mới.',
    duration: 102, releaseDate: '2026-06-14', language: 'Tiếng Anh', country: 'Mỹ',
    genres: ['Hoạt hình', 'Tâm lý', 'Gia đình'],
  },
  {
    id: 4, title: 'Twisters', originalTitle: 'Twisters',
    poster: 'https://image.tmdb.org/t/p/w500/7nGf6YVcs3rA7nTfBisT6VfGzXs.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/lF1pBAFxnQ2JrMD3IkIln1IFMJs.jpg',
    overview: 'Những thợ săn bão đối mặt với lốc xoáy siêu cường.',
    duration: 117, releaseDate: '2026-07-19', language: 'Tiếng Anh', country: 'Mỹ',
    genres: ['Hành động', 'Phiêu lưu'],
  },
  {
    id: 5, title: 'A Quiet Place: Day One', originalTitle: 'A Quiet Place: Day One',
    poster: 'https://image.tmdb.org/t/p/w500/5YF6oEfPGR5GJbkYp4TYiF9Lp3U.jpg',
    backdrop: 'https://image.tmdb.org/t/p/w1280/2xLk2HJXU0CzVJwKkEEgvn82X9K.jpg',
    overview: 'Ngày đầu tiên khi sinh vật ngoài hành tinh tấn công New York.',
    duration: 100, releaseDate: '2026-08-25', language: 'Tiếng Anh', country: 'Mỹ',
    genres: ['Kinh dị', 'Viễn tưởng'],
  },
];

function generateSeats() {
  const rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
  const seats = [];
  let id = 1;
  for (const row of rows) {
    for (let col = 1; col <= 10; col++) {
      seats.push({
        id, rowLabel: row, seatNumber: `${row}${col}`,
        status: 'AVAILABLE', seatTemplateId: id,
        seatType: id % 7 === 0 ? 'COUPLE' : 'NORMAL',
      });
      id++;
    }
  }
  seats[3].status = 'BOOKED'; seats[7].status = 'BOOKED';
  seats[12].status = 'HOLD'; seats[15].status = 'HOLD';
  seats[24].status = 'BOOKED'; seats[28].status = 'BOOKED'; seats[31].status = 'BOOKED';
  return seats;
}

export function getShowtimes(theaterId: number) {
  const baseTimes = ['08:30', '10:30', '13:00', '14:30', '16:45', '19:00', '21:30'];
  const today = new Date();
  const showtimes = [];
  let id = theaterId * 100;

  for (const movie of MOCK_MOVIES) {
    for (let day = 0; day < 3; day++) {
      const date = new Date(today);
      date.setDate(date.getDate() + day);
      const slots = baseTimes.slice(0, 3 + day);
      for (const time of slots) {
        id++;
        const [h, m] = time.split(':').map(Number);
        const showTime = new Date(date);
        showTime.setHours(h, m, 0, 0);
        showtimes.push({
          id, movieId: movie.id, movieTitle: movie.title,
          moviePosterUrl: movie.poster,
          theaterId, theaterName: MOCK_THEATERS.find(t => t.id === theaterId)?.name || '',
          theaterLocation: '',
          showDate: new Date(date), showTime,
          price: 65000 + (day * 5000) + (id % 3) * 10000,
          totalSeats: 80, availableSeats: 20 + (id % 60),
        });
      }
    }
  }
  return showtimes;
}

export const MOCK_SEATS = generateSeats();

export const MOCK_USERS = [
  { id: 1, email: 'admin@cineplex.vn', password: '123456', name: 'Admin', role: 'ADMIN' },
  { id: 1, email: 'admin@cineplex.com', password: '123456', name: 'Admin', role: 'ADMIN' },
  { id: 2, email: 'admin', password: 'admin', name: 'Admin', role: 'ADMIN' },
];

export function getJwtPayload() {
  return { userId: 1, email: 'admin@cineplex.vn', role: 'ADMIN', sub: 'admin@cineplex.vn' };
}

export const MOCK_JWT_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoiYWRtaW5AY2luZXBsZXgudm4iLCJ1c2VybmFtZSI6IkFkbWluIiwicm9sZXMiOlsiQURNSU4iXSwiZXhwIjo5OTk5OTk5OTk5fQ.mocksig';

export const MOCK_RESERVATION_INIT = {
  reservationId: 'mock-res-1', showtimeId: 101, ttl: 600,
  expiredAt: new Date(Date.now() + 600000),
};

export const MOCK_RESERVATION = {
  id: 'mock-res-1', showtimeId: 101, userId: 1, theaterId: 1,
  status: 'PENDING', totalPrice: 130000, totalSeats: 2,
  ticketCode: 'TICKET-MOCK-001',
};
