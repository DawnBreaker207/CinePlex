import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  HttpInterceptorFn,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideMockStore } from '@ngrx/store/testing';
import { Store } from '@ngrx/store';

import { AuthInterceptor } from './auth.interceptor';
import { AuthService } from '../auth/auth.service';
import { AuthActions } from '../auth/auth.actions';

const b64url = (input: string) =>
  btoa(unescape(encodeURIComponent(input)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');

const makeToken = (expOffsetSec: number) =>
  `${b64url(JSON.stringify({ alg: 'none', typ: 'JWT' }))}.${b64url(
    JSON.stringify({
      sub: 1,
      username: 'admin',
      email: 'admin@cineplex.vn',
      roles: ['ADMIN'],
      exp: Math.floor(Date.now() / 1000) + expOffsetSec,
    }),
  )}.mock-sig`;

const error401 = (message: string) => ({
  body: { code: 'ERROR', message },
  status: 401,
  statusText: 'Unauthorized',
});

describe('AuthInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => AuthInterceptor(req, next));

  let authService: AuthService;
  let ctrl: HttpTestingController;
  let store: Store;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([AuthInterceptor])),
        provideHttpClientTesting(),
        provideMockStore(),
      ],
    });
    authService = TestBed.inject(AuthService);
    ctrl = TestBed.inject(HttpTestingController);
    store = TestBed.inject(Store);
  });

  afterEach(() => ctrl.verify());

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });

  it('attaches Bearer token from signal to /api requests', () => {
    const token = makeToken(600);
    authService.setAccessToken(token);
    const http = TestBed.inject(HttpClient);

    http.get('/api/v1/movie').subscribe();

    const req = ctrl.expectOne('/api/v1/movie');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    req.flush({ code: 'OK', message: 'success', data: [] });
  });

  it('on 401 expired → silent refresh → retries the request with new token', () => {
    authService.setAccessToken(makeToken(-10));
    const http = TestBed.inject(HttpClient);
    const newToken = makeToken(600);

    http.get('/api/v1/movie').subscribe();

    ctrl
      .expectOne('/api/v1/movie')
      .flush(error401('Access token expired').body, { status: 401, statusText: 'Unauthorized' });

    const refresh = ctrl.expectOne('/api/v1/auth/refresh-token');
    refresh.flush({
      code: 'OK',
      message: 'success',
      data: { accessToken: newToken },
    });

    const retried = ctrl.expectOne('/api/v1/movie');
    expect(retried.request.headers.get('Authorization')).toBe(
      `Bearer ${newToken}`,
    );
    retried.flush({ code: 'OK', message: 'success', data: [] });

    expect(authService.accessToken()).toBe(newToken);
  });

  it('on 401 with invalid/not-found message → logout immediately, no refresh, no retry', () => {
    authService.setAccessToken(makeToken(600));
    const http = TestBed.inject(HttpClient);
    const dispatchSpy = spyOn(store, 'dispatch');

    let errored = false;
    http.get('/api/v1/movie').subscribe({ error: () => (errored = true) });

    ctrl.expectOne('/api/v1/movie').flush(error401('Refresh token not found').body, { status: 401, statusText: 'Unauthorized' });

    expect(errored).toBeTrue();
    ctrl.expectNone('/api/v1/auth/refresh-token');
    expect(dispatchSpy).toHaveBeenCalledWith(AuthActions.loadLogout());
    expect(authService.accessToken()).toBeNull();
  });

  it('on refresh failure → logout and propagate the original error', () => {
    authService.setAccessToken(makeToken(-10));
    const http = TestBed.inject(HttpClient);
    const dispatchSpy = spyOn(store, 'dispatch');

    let status = 0;
    http
      .get('/api/v1/movie')
      .subscribe({ error: (e: HttpErrorResponse) => (status = e.status) });

    ctrl.expectOne('/api/v1/movie').flush(error401('Access token expired').body, { status: 401, statusText: 'Unauthorized' });
    ctrl
      .expectOne('/api/v1/auth/refresh-token')
      .flush(error401('Refresh token expired').body, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
    expect(dispatchSpy).toHaveBeenCalledWith(AuthActions.loadLogout());
    expect(authService.accessToken()).toBeNull();
  });

  it('does NOT refresh on 401 from login request', () => {
    const http = TestBed.inject(HttpClient);
    http
      .post('/api/v1/auth/login', {})
      .subscribe({ error: () => undefined });

    ctrl
      .expectOne('/api/v1/auth/login')
      .flush(error401('Sai email hoặc mật khẩu').body, { status: 401, statusText: 'Unauthorized' });

    ctrl.expectNone('/api/v1/auth/refresh-token');
  });
});
