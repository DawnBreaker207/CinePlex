import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { NetworkService } from './network.service';
import { StorageService } from '@core/services/storage/storage.service';
import { ToastService } from '@core/services/toast/toast.service';
import { environment } from '@env/environment';

describe('NetworkService (S2 pending-payment survival)', () => {
  let http: HttpTestingController;
  let storage: StorageService;
  let routerSpy: { navigate: jasmine.Spy };
  let toastSpy: { createNotification: jasmine.Spy };

  const recheckUrl = (provider: string, id: string) =>
    `${environment.apiUrl}/payment/re-check/${provider}/${id}`;

  beforeEach(() => {
    localStorage.clear();
    routerSpy = { navigate: jasmine.createSpy('navigate') };
    toastSpy = { createNotification: jasmine.createSpy('createNotification') };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        { provide: ToastService, useValue: toastSpy },
        {
          provide: TranslateService,
          useValue: { instant: (key: string) => key },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    storage = TestBed.inject(StorageService);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('creates without HTTP when no payment is pending', () => {
    TestBed.inject(NetworkService);
    http.expectNone(recheckUrl('mock', 'ANY'));
  });

  it('boot check: PAID clears the marker and routes to the result', () => {
    storage.setItem('lastPayment', {
      provider: 'MOCK',
      reservationId: 'CP-1',
    });

    TestBed.inject(NetworkService);
    const req = http.expectOne(recheckUrl('mock', 'CP-1'));
    expect(req.request.method).toBe('POST');
    req.flush({ code: 'ok', message: 'ok', data: 'PAID' });

    expect(storage.getItem('lastPayment')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/paymentResult'], {
      queryParams: { status: 'success', reservationId: 'CP-1' },
    });
    expect(toastSpy.createNotification).toHaveBeenCalledWith(
      jasmine.objectContaining({ type: 'success' }),
    );
  });

  it('boot check is quiet when still pending', () => {
    storage.setItem('lastPayment', {
      provider: 'MOCK',
      reservationId: 'CP-1',
    });

    TestBed.inject(NetworkService);
    http.expectOne(recheckUrl('mock', 'CP-1')).flush({
      code: 'ok',
      message: 'ok',
      data: 'PENDING',
    });

    expect(toastSpy.createNotification).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(storage.getItem('lastPayment')).not.toBeNull();
  });

  it('online event re-checks and announces a still-pending payment', () => {
    storage.setItem('lastPayment', {
      provider: 'MOCK',
      reservationId: 'CP-1',
    });
    TestBed.inject(NetworkService);
    // Boot check (quiet).
    http.expectOne(recheckUrl('mock', 'CP-1')).flush({
      code: 'ok',
      message: 'ok',
      data: 'PENDING',
    });
    expect(toastSpy.createNotification).not.toHaveBeenCalled();

    window.dispatchEvent(new Event('online'));
    http.expectOne(recheckUrl('mock', 'CP-1')).flush({
      code: 'ok',
      message: 'ok',
      data: 'PENDING',
    });

    expect(toastSpy.createNotification).toHaveBeenCalledWith(
      jasmine.objectContaining({ type: 'info' }),
    );
  });
});
