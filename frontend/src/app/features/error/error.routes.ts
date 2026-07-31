import { Routes } from '@angular/router';
import { Error404Component } from '@features/error/404/error-404.component';
import { Error500Component } from '@features/error/500/error-500.component';
import { ErrorLayoutComponent } from '../../layout/error-layout/error-layout.component';

export const ERROR_ROUTES: Routes = [
  {
    path: '',
    component: ErrorLayoutComponent,
    children: [
      { path: '', redirectTo: '404', pathMatch: 'full' },
      { path: '404', title: 'error.404', component: Error404Component },
      { path: '500', title: 'error.500', component: Error500Component },
    ],
  },
  { path: '**', redirectTo: '404' },
];
