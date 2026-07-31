import { TestBed } from '@angular/core/testing';

import { DashboardService } from './dashboard.service';

import { testProviders } from '@shared/testing/test-providers';

describe('DashboardService', () => {
  let service: DashboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(DashboardService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
