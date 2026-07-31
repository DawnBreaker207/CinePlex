import { TestBed } from '@angular/core/testing';

import { TheaterService } from './theater.service';

import { testProviders } from '@shared/testing/test-providers';

describe('TheaterService', () => {
  let service: TheaterService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(TheaterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
