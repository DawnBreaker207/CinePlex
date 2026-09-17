import { Routes } from '@angular/router';
import { AdminGuard } from '@core/guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'client', pathMatch: 'full' },
  {
    path: '',
    loadChildren: () =>
      import('./features/client/client.routes').then((m) => m.CLIENT_ROUTES),
  },
  {
    path: 'admin',
    canMatch: [AdminGuard],
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'errors',
    loadChildren: () =>
      import('./features/error/error.routes').then((m) => m.ERROR_ROUTES),
  },
  { path: '**', redirectTo: 'errors/404' },
];
