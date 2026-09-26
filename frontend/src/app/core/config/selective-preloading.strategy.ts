import { Injectable } from '@angular/core';
import { PreloadingStrategy, Route } from '@angular/router';
import { Observable, of } from 'rxjs';

/**
 * Preload everything except the admin bundle: guests never pay for code
 * only staff can reach (AdminGuard blocks it anyway). Admin chunks load
 * on demand after login.
 */
@Injectable({ providedIn: 'root' })
export class SelectivePreloadingStrategy implements PreloadingStrategy {
  preload(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    return route.path === 'admin' ? of(null) : load();
  }
}
