import { TestBed } from '@angular/core/testing';

import { ReportService } from './report.service';

import { testProviders } from '@shared/testing/test-providers';

describe('ReportService', () => {
  let service: ReportService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(ReportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
