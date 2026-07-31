import { TestBed } from '@angular/core/testing';

import { ReservationService } from './reservation.service';

import { testProviders } from '@shared/testing/test-providers';

describe('Reservation', () => {
  let service: ReservationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(ReservationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
