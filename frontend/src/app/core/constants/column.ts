import { ReservationStatus } from '@core/constants/enum';

const marker = (key: string) => key;

export const HEADER_COLUMNS = {
  MOVIE: [
    marker('common.poster'),
    marker('common.info'),
    marker('common.genre'),
    marker('common.status'),
    marker('common.actions'),
  ],
  THEATER: [
    '#',
    marker('common.name'),
    marker('common.location'),
    marker('common.capacity'),
    marker('booking.showtime'),
    marker('common.createdAt'),
    marker('common.actions'),
  ],
  SHOWTIME: [
    marker('common.movieInfo'),
    marker('common.showtime'),
    marker('common.location'),
    marker('common.status'),
    marker('common.actions'),
  ],
  RESEVATION: [
    '#',
    marker('common.status'),
    marker('reservation.total'),
    marker('common.createdAt'),
    marker('common.actions'),
  ],
  USER: [
    marker('common.info'),
    marker('common.status'),
    marker('common.actions'),
  ],
  ARTICLE: [
    marker('common.image'),
    marker('common.title'),
    marker('common.category'),
    marker('common.author'),
    marker('common.status'),
    marker('common.createdAt'),
    marker('common.actions'),
  ],
  VOUCHER: [
    marker('common.info'),
    marker('common.time'),
    marker('voucher.discountRate'),
    marker('voucher.condition'),
    marker('voucher.scope'),
    marker('common.status'),
    marker('common.actions'),
  ],
};

export const HEADERS = [{ name: marker('common.movies'), path: '/movies' }];

export const PROFILE_MENU = [
  {
    title: marker('profileMenu.admin'),
    icon: 'Gauge',
    link: '/admin',
    role: 'admin',
  },
  {
    title: marker('profile.title'),
    icon: 'CircleUserRound',
    link: '/profile',
    role: 'user',
  },
  {
    title: marker('favorites.title'),
    icon: 'Heart',
    link: '/favorites',
    role: 'both',
  },
  {
    title: marker('common.logout'),
    icon: 'LogOut',
    action: 'logout',
    isDanger: true,
    role: 'both',
  },
];

export const RESERVATION_STATUS_LABELS: Record<ReservationStatus, string> = {
  CONFIRMED: marker('status.confirmed'),
  CANCELED: marker('status.cancelled'),
  PENDING: marker('status.pending'),
  REFUNDED: marker('status.refunded'),
};

export const RESERVATION_STATUS = [
  { value: 'CONFIRMED', label: RESERVATION_STATUS_LABELS.CONFIRMED },
  { value: 'CANCELED', label: RESERVATION_STATUS_LABELS.CANCELED },
  { value: 'PENDING', label: RESERVATION_STATUS_LABELS.PENDING },
  { value: 'REFUNDED', label: RESERVATION_STATUS_LABELS.REFUNDED },
];
