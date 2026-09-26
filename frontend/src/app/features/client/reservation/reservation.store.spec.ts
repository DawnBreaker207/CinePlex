import {
  TestBed,
  fakeAsync,
  tick,
} from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { TranslateService } from '@ngx-translate/core';

import { ReservationStore } from './reservation.store';
import { SeatStore } from './components/seat/seat.store';
import { selectJwt } from '@core/auth/auth.selectors';
import { selectSelectedShowtime } from '@domain/showtime/data-access/showtime.selectors';
import { selectSelectedTheater } from '@domain/theater/data-access/theater.selectors';
import { environment } from '@env/environment';

describe('ReservationStore (S1 heartbeat, S4 guard, C4 snapshot)', () => {
  let http: HttpTestingController;
  let mockStore: MockStore;
  let store: InstanceType<typeof ReservationStore>;
  let routerSpy: { navigate: jasmine.Spy };
  const seatStub = {
    selectedSeats: () => [
      { id: 11 },
      { id: 12 },
    ],
    selectSeatsCount: () => 2,
    totalPrice: () => 200000,
  };

  const resUrl = `${environment.apiUrl}/reservation`;
  const payUrl = `${environment.apiUrl}/payment`;

  beforeEach(() => {
    localStorage.clear();
    routerSpy = { navigate: jasmine.createSpy('navigate') };
    spyOn(window, 'open').and.stub();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideMockStore(),
        { provide: Router, useValue: routerSpy },
        { provide: SeatStore, useValue: seatStub },
        {
          provide: TranslateService,
          useValue: { instant: (key: string) => key },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    mockStore = TestBed.inject(MockStore);
    mockStore.overrideSelector(selectJwt, { userId: 7 } as never);
    mockStore.overrideSelector(selectSelectedShowtime, {
      id: 513,
    } as never);
    mockStore.overrideSelector(selectSelectedTheater, { id: 1 } as never);
    store = TestBed.inject(ReservationStore);
  });
  afterEach(() => {
    (store as unknown as { _cleanup: () => void })._cleanup();
    http.verify();
    localStorage.clear();
  });

  function initReservation() {
    store.initReservation({ userId: 7, showtimeId: 513, theaterId: 1 });
    http.expectOne(`${resUrl}/init`).flush({
      code: 'ok',
      message: 'ok',
      data: {
        reservationCode: 'CP-1',
        showtimeId: 513,
        ttl: 900,
        expiredAt: new Date().toISOString(),
      },
    });
  }

  it('pay() sends the paid seatIds snapshot (C4)', fakeAsync(() => {
    initReservation();
    tick();

    store.pay('MOCK');
    const req = http.expectOne(
      (r) => r.url === `${payUrl}/create` && r.method === 'GET',
    );
    expect(req.request.params.getAll('seatIds')).toEqual(['11', '12']);
    req.flush({
      code: 'ok',
      message: 'ok',
      data: { paymentUrl: 'http://gateway/pay' },
    });
    tick();
    expect(window.open).toHaveBeenCalledWith('http://gateway/pay', '_self');
  }));

  it('pay() 409 clears the dead session and routes home (S4)', fakeAsync(() => {
    initReservation();
    tick();

    store.pay('MOCK');
    http
      .expectOne((r) => r.url === `${payUrl}/create`)
      .flush('expired', { status: 409, statusText: 'Conflict' });
    tick();

    expect(store.paymentError()).toBe('reservation.expired');
    expect(store.reservationId()).toBeNull();
    expect(localStorage.getItem('reservationState')).toBeNull();
    expect(localStorage.getItem('lastPayment')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/home']);
  }));

  it('heartbeat re-holds silently every 45s without spinner flags (S1)', fakeAsync(() => {
    initReservation();
    tick();

    tick(45000);
    const open1 = http.match(`${resUrl}/seatHold`);
    expect(open1.length).toBe(1);
    expect(open1[0].request.method).toBe('POST');
    // Silent refresh: no loading/saving spinner tripped.
    expect(store.loading()).toBe(false);
    open1[0].flush({ code: 'ok', message: 'ok', data: null });
    tick();

    tick(45000);
    const open2 = http.match(`${resUrl}/seatHold`);
    // eslint-disable-next-line no-console
    console.log('DBG open2=' + open2.length + ' ttl=' + store.currentTTL()?.ttl);
    expect(open2.length).toBe(1);
    open2[0].flush({
      code: 'ok',
      message: 'ok',
      data: null,
    });
    tick();
    (store as unknown as { _cleanup: () => void })._cleanup();
    tick();
    expect(http.match(`${resUrl}/seatHold`).length).toBe(0);
  }));
});
