import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { mockInterceptor } from './mock-interceptor';

describe('MockInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([mockInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('login with identifier returns Jwt object', (done) => {
    http.post<any>('/api/v1/auth/login', { identifier: 'admin@cineplex.vn', password: '123456' }).subscribe({
      next: (res) => {
        expect(res).toBeTruthy();
        expect(res.code).toBe('OK');
        expect(res.data).toBeTruthy();
        expect(res.data.userId).toBe(1);
        expect(res.data.username).toBe('Admin');
        expect(res.data.email).toBe('admin@cineplex.vn');
        expect(res.data.accessToken).toBeTruthy();
        done();
      },
      error: () => {
        fail('login should not error');
        done();
      },
    });
  });

  it('login with wrong password returns 401', (done) => {
    http.post('/api/v1/auth/login', { identifier: 'admin@cineplex.vn', password: 'wrong' }).subscribe({
      next: () => {
        fail('should have errored');
        done();
      },
      error: (err) => {
        expect(err.status).toBe(401);
        done();
      },
    });
  });
});
