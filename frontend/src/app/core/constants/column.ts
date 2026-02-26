import { ReservationStatus } from '@core/constants/enum';

const marker = (key: string) => key;

export const HEADER_COLUMNS = {
  MOVIE: [
    marker('Poster'),
    marker('Thông tin'),
    marker('Thể loại'),
    marker('Trạng thái'),
    marker('Hành động'),
  ],
  THEATER: [
    '#',
    marker('Name'),
    marker('Vị trí'),
    marker('Sức chứa'),
    marker('Giờ chiếu'),
    marker('Ngày tạo'),
    marker('Hành động'),
  ],
  SHOWTIME: [
    marker('Thông tin phim'),
    marker('Lịch chiếu'),
    marker('Vị trí'),
    marker('Trạng thái'),
    marker('Hành động'),
  ],
  RESEVATION: [
    '#',
    marker('Trạng thái'),
    marker('Tổng cộng'),
    marker('Ngày tạo'),
    marker('Hành động'),
  ],
  USER: [marker('Thông tin'), marker('Trạng thái'), marker('Hành động')],
  ARTICLE: [
    marker('Ảnh'),
    marker('Tiêu đề'),
    marker('Danh mục'),
    marker('Tác giả'),
    marker('Trạng thái'),
    marker('Ngày tạo'),
    marker('Hành động'),
  ],
  VOUCHER: [
    marker('Thông tin'),
    marker('Thởi gian'),
    marker('Mức giảm'),
    marker('Tình trạng'),
    marker('Phạm vi'),
    marker('Trạng thái'),
    marker('Hành động'),
  ],
};

export const HEADERS = [
  { name: marker('Phim'), path: '/client' },
  { name: marker('Rạp'), path: '' },
  { name: marker('Tin mới và ưu đãi'), path: '' },
];

export const PROFILE_MENU = [
  {
    title: marker('Quản trị viên'),
    icon: 'Gauge',
    link: '/admin',
    role: 'admin',
  },
  {
    title: marker('Hồ sơ cá nhân'),
    icon: 'CircleUserRound',
    link: '/profile',
    role: 'user',
  },
  {
    title: marker('Cài đặt'),
    icon: 'Settings',
    link: '/settings',
    role: 'user',
  },
  {
    title: marker('Đăng xuất'),
    icon: 'LogOut',
    action: 'logout',
    isDanger: true,
    role: 'both',
  },
];

export const RESERVATION_STATUS_LABELS: Record<ReservationStatus, string> = {
  CONFIRMED: marker('Đã xác nhận'),
  CANCELED: marker('Đã hủy'),
};

export const RESERVATION_STATUS = [
  { value: 'CONFIRMED', label: RESERVATION_STATUS_LABELS.CONFIRMED },
  { value: 'CANCELED', label: RESERVATION_STATUS_LABELS.CANCELED },
];
