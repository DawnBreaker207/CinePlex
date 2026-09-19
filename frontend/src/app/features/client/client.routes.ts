import { Routes } from '@angular/router';
import { MainLayoutComponent } from '../../layout/main-layout/main-layout.component';
import { HomeComponent } from '@features/client/home/home.component';
import { MoviesComponent } from '@features/client/movies/movies.component';
import { DetailComponent } from '@features/client/detail/detail.component';
import { AuthComponent } from '@features/auth/auth';
import { VerifyEmailComponent } from '@features/auth/verify-email/verify-email.component';
import { AuthGuard } from '@core/guards/auth.guard';
import { canDeactivateReservation } from '@core/guards/can-deactivate-reservation.guard';
import { ReservationComponent } from '@features/client/reservation/reservation.component';
import { PaymentResultComponent } from '@features/client/reservation/components/payment-result/payment-result.component';
import { ProfileComponent } from '@features/client/profile/profile.component';
import { FavoritesComponent } from '@features/client/favorites/favorites.component';
import { ShowtimesComponent } from '@features/client/showtimes/showtimes.component';
import { TheatersComponent } from '@features/client/theaters/theaters.component';
import { PricingComponent } from '@features/client/pricing/pricing.component';
import { NewsComponent } from '@features/client/news/news.component';
import { RedirectGuard } from '@core/guards/redirect.guard';

export const CLIENT_ROUTES: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      {
        path: 'home',
        title: 'client.home',
        component: HomeComponent,
      },
      {
        path: 'movies',
        title: 'client.movies',
        component: MoviesComponent,
      },
      {
        path: 'showtimes',
        title: 'nav.showtimes',
        component: ShowtimesComponent,
      },
      {
        path: 'theaters',
        title: 'nav.theaters',
        component: TheatersComponent,
      },
      {
        path: 'pricing',
        title: 'nav.pricing',
        component: PricingComponent,
      },
      {
        path: 'news',
        title: 'nav.news',
        component: NewsComponent,
      },
      {
        path: 'profile',
        title: 'client.profile',
        canMatch: [AuthGuard],
        component: ProfileComponent,
      },
      {
        path: 'favorites',
        title: 'client.favorites',
        component: FavoritesComponent,
      },
      {
        path: 'movie/detail/:id',
        title: 'client.movieDetail',
        component: DetailComponent,
      },
      {
        path: 'login',
        title: 'client.login',
        component: AuthComponent,
        canMatch: [RedirectGuard],
      },
      {
        path: 'verify-email',
        component: VerifyEmailComponent,
      },
      {
        path: 'reservation/:reservationId/:showtimeId',
        title: 'client.reservation',
        canMatch: [AuthGuard],
        canDeactivate: [canDeactivateReservation],
        component: ReservationComponent,
      },
      { path: 'paymentResult', title: 'client.paymentResult', component: PaymentResultComponent },
    ],
  },
];
