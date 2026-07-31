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
import { CheckInComponent } from '@features/admin/check-in/check-in.component';

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
        title: 'admin.dashboard',
        component: DashboardComponent,
      },
      {
        path: 'movie',
        title: 'admin.movie',
        component: MovieComponent,
      },
      {
        path: 'theater',
        title: 'admin.theater',
        component: TheaterComponent,
      },
      {
        path: 'showtime',
        title: 'admin.showtime',
        component: ShowtimeComponent,
      },
      {
        path: 'reservation',
        title: 'admin.reservation',
        component: ReservationComponent,
      },
      {
        path: 'checkin',
        title: 'admin.checkin',
        component: CheckInComponent,
      },
      {
        path: 'user',
        title: 'admin.user',
        component: UserComponent,
      },
      {
        path: 'article',
        title: 'admin.article',
        component: ArticleComponent,
      },
      {
        path: 'voucher',
        title: 'admin.voucher',
        component: VoucherListComponent,
      },
    ],
  },
];
