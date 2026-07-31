import { TestBed } from '@angular/core/testing';
import { CanMatchFn } from '@angular/router';

import { AuthGuard } from './auth.guard';

import { testProviders } from '@shared/testing/test-providers';

describe('AuthGuard', () => {
  const executeGuard: CanMatchFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => AuthGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
