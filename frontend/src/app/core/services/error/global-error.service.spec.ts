import { TestBed } from '@angular/core/testing';

import { GlobalErrorService } from './global-error.service';

import { testProviders } from '@shared/testing/test-providers';

describe('GlobalErrorService', () => {
  let service: GlobalErrorService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(GlobalErrorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
