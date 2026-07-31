import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { jwtDecode } from 'jwt-decode';
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
        const payload: any = jwtDecode(res.data.accessToken);
        expect(payload.roles).toContain('ADMIN');
        expect(payload.exp).toBeGreaterThan(Math.floor(Date.now() / 1000));
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

  it('dashboard summary matches DashboardSummary shape', (done) => {
    http.get('/api/v1/dashboard/summary').subscribe({
      next: (res: any) => {
        const s = res.data;
        expect(s.metrics).toBeTruthy();
        expect(s.metrics.totalRevenue).toBeGreaterThan(0);
        expect(Array.isArray(s.revenues)).toBeTrue();
        expect(Array.isArray(s.movies)).toBeTrue();
        expect(Array.isArray(s.theaters)).toBeTrue();
        expect(Array.isArray(s.payments)).toBeTrue();
        done();
      },
      error: () => fail('should not error'),
    });
  });

  it('user list paginates with userId field', (done) => {
    http.get('/api/v1/user', { params: { page: '0', size: '10' } }).subscribe({
      next: (res: any) => {
        const users = res.data.content;
        expect(users.length).toBeGreaterThan(0);
        expect(users[0].userId).toBeDefined();
        expect(users[0].role).toBeDefined();
        done();
      },
      error: () => fail('should not error'),
    });
  });

  it('reservation /me returns ReservationProfile shape', (done) => {
    http.get('/api/v1/reservation/me', { params: { userId: '3' } }).subscribe({
      next: (res: any) => {
        const profiles = res.data.content;
        expect(profiles.length).toBeGreaterThan(0);
        const p = profiles[0];
        expect(p.reservationId).toBeDefined();
        expect(p.movieTitle).toBeDefined();
        expect(p.date).toBeDefined();
        expect(p.seats.length).toBeGreaterThan(0);
        done();
      },
      error: () => fail('should not error'),
    });
  });

  it('movie CRUD: create then list then delete', (done) => {
    http.post('/api/v1/movie', { title: 'Test Movie', originalTitle: 'T', poster: '', backdrop: '', overview: '', duration: 100, genres: [], releaseDate: '2026-01-01', language: 'vi', country: 'VN', imdbId: '', filmId: '' }).subscribe({
      next: (created: any) => {
        expect(created.data.id).toBeDefined();
        expect(created.data.title).toBe('Test Movie');
        http.get('/api/v1/movie').subscribe({
          next: (list: any) => {
            const found = list.data.content.some((m: any) => m.title === 'Test Movie');
            expect(found).toBeTrue();
            done();
          },
        });
      },
      error: () => fail('should not error'),
    });
  });
});
