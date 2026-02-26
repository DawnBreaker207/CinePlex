import { Routes } from '@angular/router';
import { DashboardComponent } from '@features/admin/dashboard/dashboard.component';
import { MovieComponent } from '@features/admin/movie/movie.component';
import { TheaterComponent } from '@features/admin/theater/theater.component';
import { ShowtimeComponent } from '@features/admin/showtime/showtime.component';
import { ReservationComponent } from '@features/admin/reservation/reservation.component';
import { UserComponent } from '@features/admin/user/user.component';
import { ArticleComponent } from '@features/admin/article/article.component';
import { AdminLayoutComponent } from '../../layout/admin-layout/admin-layout.component';
import { VoucherComponent } from '@features/admin/voucher/voucher.component';

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
        component: DashboardComponent,
        data: { breadcrumb: 'Dashboard' },
      },
      {
        path: 'movie',
        component: MovieComponent,
        data: { breadcrumb: 'Movie Manager' },
      },
      {
        path: 'theater',
        component: TheaterComponent,
        data: { breadcrumb: 'Theater Manager' },
      },
      {
        path: 'showtime',
        component: ShowtimeComponent,
        data: { breadcrumb: 'Showtime Manager' },
      },
      {
        path: 'reservation',
        component: ReservationComponent,
        data: { breadcrumb: 'Reservation Manager' },
      },
      {
        path: 'user',
        component: UserComponent,
        data: { breadcrumb: 'User Manager' },
      },
      {
        path: 'article',
        component: ArticleComponent,
        data: { breadcrumb: 'Article Manager' },
      },
      {
        path: 'voucher',
        component: VoucherComponent,
        data: { breadcrumb: 'Voucher Manager' },
      },
    ],
  },
];
