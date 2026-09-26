import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { PaymentResultComponent } from './payment-result.component';

import { testProviders } from '@shared/testing/test-providers';
import { ToastService } from '@core/services/toast/toast.service';
import { StorageService } from '@core/services/storage/storage.service';
import { environment } from '@env/environment';

describe('PaymentResultComponent', () => {
  let component: PaymentResultComponent;
  let fixture: ComponentFixture<PaymentResultComponent>;
  let http: HttpTestingController;
  let storage: StorageService;
  let toastSpy: { createNotification: jasmine.Spy };

  const recheckUrl = `${environment.apiUrl}/payment/re-check/mock/CP-1`;

  function setup(queryParams: Record<string, string> = {}) {
    toastSpy = { createNotification: jasmine.createSpy('createNotification') };
    TestBed.configureTestingModule({
      imports: [PaymentResultComponent],
      providers: [
        testProviders,
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: { navigate: jasmine.createSpy() } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParams } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    storage = TestBed.inject(StorageService);
    fixture = TestBed.createComponent(PaymentResultComponent);
    component = fixture.componentInstance;
  }

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('should create', () => {
    setup();
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('auto-poll re-checks quietly and flips to success on PAID (S2)', fakeAsync(() => {
    setup({ reservationId: 'CP-1', status: 'failed' });
    storage.setItem('reservationState', {
      reservationId: 'CP-1',
      userId: 7,
      showtimeId: 513,
      theaterId: 1,
    });
    storage.setItem('lastPayment', {
      provider: 'MOCK',
      reservationId: 'CP-1',
    });
    fixture.detectChanges();

    // First poll: still pending -> quiet, no toast, keeps polling.
    tick(4000);
    http.expectOne(recheckUrl).flush({
      code: 'ok',
      message: 'ok',
      data: 'PENDING',
    });
    tick();
    expect(toastSpy.createNotification).not.toHaveBeenCalled();
    expect(component.status()).toBe('error');

    // Second poll: paid -> success, marker cleared, polling stops.
    tick(4000);
    http.expectOne(recheckUrl).flush({
      code: 'ok',
      message: 'ok',
      data: 'PAID',
    });
    tick();
    expect(component.status()).toBe('success');
    expect(storage.getItem('lastPayment')).toBeNull();
    tick(12000);
    http.expectNone(recheckUrl);
    fixture.destroy();
  }));

  it('missing SPA state resolves from the server via lastPayment (S2)', fakeAsync(() => {
    setup({ reservationId: 'CP-1', status: 'success' });
    storage.setItem('lastPayment', {
      provider: 'MOCK',
      reservationId: 'CP-1',
    });
    fixture.detectChanges();
    tick();

    http.expectOne(recheckUrl).flush({
      code: 'ok',
      message: 'ok',
      data: 'PAID',
    });
    tick();
    expect(component.status()).toBe('success');
    fixture.destroy();
  }));
});
