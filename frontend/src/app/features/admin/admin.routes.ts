import { Routes } from '@angular/router';
import { DashboardComponent } from '@features/admin/dashboard/dashboard.component';
import { MovieComponent } from '@features/admin/movie/movie.component';
import { TheaterComponent } from '@features/admin/theater/theater.component';
import { ShowtimeComponent } from '@features/admin/showtime/showtime.component';
import { ReservationComponent } from '@features/admin/reservation/reservation.component';
import { UserComponent } from '@features/admin/user/user.component';
import { ArticleComponent } from '@features/admin/article/article.component';
import { AdminLayoutComponent } from '../../layout/admin-layout/admin-layout.component';
import { VoucherListComponent } from '@features/admin/voucher/pages/voucher-list/voucher-list.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        title: 'Thống kê',
        component: DashboardComponent,
      },
      {
        path: 'movie',
        title: 'Quản lý phim',
        component: MovieComponent,
      },
      {
        path: 'theater',
        title: 'Quản lý rạp',
        component: TheaterComponent,
      },
      {
        path: 'showtime',
        title: 'Quản lý suất chiếu',
        component: ShowtimeComponent,
      },
      {
        path: 'reservation',
        title: 'Quản lý vé đặt',
        component: ReservationComponent,
      },
      {
        path: 'user',
        title: 'Quản lý người dùng',
        component: UserComponent,
      },
      {
        path: 'article',
        title: 'Quản lý bài viết',
        component: ArticleComponent,
      },
      {
        path: 'voucher',
        title: 'Quản lý voucher',
        component: VoucherListComponent,
      },
    ],
  },
];
