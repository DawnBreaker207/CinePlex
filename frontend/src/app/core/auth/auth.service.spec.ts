import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { AuthService } from './auth.service';

import { testProviders } from '@shared/testing/test-providers';
import { environment } from '@env/environment';

interface ApiRes<T> {
  code: string;
  message: string;
  data: T;
}

const b64url = (input: string) =>
  btoa(unescape(encodeURIComponent(input)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');

const makeToken = (expOffsetSec: number, sub = 1) =>
  `${b64url(JSON.stringify({ alg: 'none', typ: 'JWT' }))}.${b64url(
    JSON.stringify({
      sub,
      username: 'admin',
      email: 'admin@cineplex.vn',
      roles: ['ADMIN'],
      exp: Math.floor(Date.now() / 1000) + expOffsetSec,
    }),
  )}.mock-sig`;

describe('AuthService', () => {
  let service: AuthService;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(AuthService);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isAuthenticated is false when no token in memory', () => {
    expect(service.accessToken()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('login stores access token in signal (not storage)', () => {
    const token = makeToken(600);
    const jwt = {
      userId: 1,
      username: 'admin',
      email: 'admin@cineplex.vn',
      accessToken: token,
    };

    service.login({
      identifier: 'admin',
      password: 'admin',
    } as never).subscribe((res) => {
      expect(res?.accessToken).toBe(token);
    });

    ctrl.expectOne(`${environment.apiUrl}/auth/login`).flush({
      code: 'OK',
      message: 'success',
      data: jwt,
    } satisfies ApiRes<typeof jwt>);

    expect(service.accessToken()).toBe(token);
    expect(service.isAuthenticated()).toBeTrue();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('jwt')).toBeNull();
  });

  it('isAuthenticated is false when access token expired', () => {
    service.setAccessToken(makeToken(-10));
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('getCurrentUser decodes payload from signal', () => {
    service.setAccessToken(makeToken(600));
    const user = service.getCurrentUser();
    expect(Number(user?.sub)).toBe(1);
    expect(user?.roles).toContain('ADMIN');
  });

  it('silentRefresh dedupes concurrent callers into one HTTP request', () => {
    const token = makeToken(600);
    let completed = 0;

    service.silentRefresh().subscribe(() => completed++);
    service.silentRefresh().subscribe(() => completed++);
    service.silentRefresh().subscribe(() => completed++);

    expect(completed).toBe(0);
    ctrl.expectOne(`${environment.apiUrl}/auth/refresh-token`).flush({
      code: 'OK',
      message: 'success',
      data: { accessToken: token },
    });
    // silentRefresh also resolves the userId by email.
    ctrl.expectOne(`${environment.apiUrl}/user/email/admin@cineplex.vn`).flush({
      code: 'OK',
      message: 'success',
      data: { userId: 1, username: 'admin', email: 'admin@cineplex.vn' },
    });

    expect(completed).toBe(3);
    expect(service.accessToken()).toBe(token);
  });

  it('silentRefresh returns null on failure without throwing', () => {
    let result: string | null | undefined;
    let failed = false;

    service
      .silentRefresh()
      .subscribe({ next: (v) => (result = v), error: () => (failed = true) });

    ctrl
      .expectOne(`${environment.apiUrl}/auth/refresh-token`)
      .error(new ErrorEvent('network'), { status: 401 });

    expect(result).toBeNull();
    expect(failed).toBeFalse();
    expect(service.accessToken()).toBeNull();
  });

  it('logout clears the in-memory token even when the call fails', () => {
    service.setAccessToken(makeToken(600));

    service.logout().subscribe({ error: () => undefined });
    ctrl
      .expectOne(`${environment.apiUrl}/auth/logout`)
      .error(new ErrorEvent('network'), { status: 500 });

    expect(service.accessToken()).toBeNull();
  });
});
