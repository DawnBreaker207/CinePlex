import { TestBed } from '@angular/core/testing';

import { SeatService } from './seat.service';

import { testProviders } from '@shared/testing/test-providers';

describe('SeatService', () => {
  let service: SeatService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(SeatService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
