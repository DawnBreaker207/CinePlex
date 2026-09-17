import { TestBed } from '@angular/core/testing';
import { CanMatchFn } from '@angular/router';

import { AdminGuard } from './admin.guard';

import { testProviders } from '@shared/testing/test-providers';

describe('AdminGuard', () => {
  const executeGuard: CanMatchFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => AdminGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
