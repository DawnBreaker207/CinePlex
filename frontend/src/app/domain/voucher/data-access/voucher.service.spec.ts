import { TestBed } from '@angular/core/testing';

import { VoucherService } from './voucher.service';

import { testProviders } from '@shared/testing/test-providers';

describe('VoucherService', () => {
  let service: VoucherService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(VoucherService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
