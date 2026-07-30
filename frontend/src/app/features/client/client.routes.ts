import { Routes } from '@angular/router';
import { MainLayoutComponent } from '../../layout/main-layout/main-layout.component';
import { HomeComponent } from '@features/client/home/home.component';
import { MoviesComponent } from '@features/client/movies/movies.component';
import { DetailComponent } from '@features/client/detail/detail.component';
import { AuthComponent } from '@features/auth/auth';
import { AuthGuard } from '@core/guards/auth.guard';
import { canDeactivateReservation } from '@core/guards/can-deactivate-reservation.guard';
import { ReservationComponent } from '@features/client/reservation/reservation.component';
import { PaymentResultComponent } from '@features/client/reservation/components/payment-result/payment-result.component';
import { ProfileComponent } from '@features/client/profile/profile.component';
import { RedirectGuard } from '@core/guards/redirect.guard';

export const CLIENT_ROUTES: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      {
        path: 'home',
        title: 'Trang chủ',
        component: HomeComponent,
      },
      {
        path: 'movies',
        title: 'Danh sách phim',
        component: MoviesComponent,
      },
      {
        path: 'profile',
        title: 'Hồ sơ',
        component: ProfileComponent,
      },
      {
        path: 'movie/detail/:id',
        title: 'Chi tiết phim',
        component: DetailComponent,
      },
      {
        path: 'login',
        title: 'Đăng nhập',
        component: AuthComponent,
        canMatch: [RedirectGuard],
      },
      {
        path: 'reservation/:reservationId/:showtimeId',
        title: 'Đặt vé',
        canMatch: [AuthGuard],
        canDeactivate: [canDeactivateReservation],
        component: ReservationComponent,
      },
      { path: 'paymentResult', title: 'Kết quả thanh toán', component: PaymentResultComponent },
    ],
  },
];
