import { TestBed } from '@angular/core/testing';

import { ShowtimeService } from './showtime.service';

import { testProviders } from '@shared/testing/test-providers';

describe('ShowtimeService', () => {
  let service: ShowtimeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(ShowtimeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
