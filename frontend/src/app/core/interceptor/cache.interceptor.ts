import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of, tap } from 'rxjs';

interface CacheEntry {
  response: HttpResponse<unknown>;
  expiry: number;
}

const cache = new Map<string, CacheEntry>();
const TTL = 300_000;

export const CacheInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET') return next(req);

  const key = `${req.url}|${req.params.toString()}`;
  const entry = cache.get(key);

  if (entry && Date.now() < entry.expiry) {
    return of(entry.response.clone());
  }

  return next(req).pipe(
    tap((event) => {
      if (event instanceof HttpResponse && event.status === 200) {
        cache.set(key, { response: event.clone(), expiry: Date.now() + TTL });
      }
    }),
  );
};
