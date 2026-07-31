import { TestBed } from '@angular/core/testing';

import { SseService } from './sse.service';

import { testProviders } from '@shared/testing/test-providers';

describe('SseService', () => {
  let service: SseService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(SseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
