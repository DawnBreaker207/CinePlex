import { TestBed } from '@angular/core/testing';

import { ToastService } from './toast.service';

import { testProviders } from '@shared/testing/test-providers';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
    service = TestBed.inject(ToastService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
