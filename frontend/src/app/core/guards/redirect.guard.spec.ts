import { TestBed } from '@angular/core/testing';
import { CanMatchFn } from '@angular/router';

import { RedirectGuard } from './redirect.guard';

import { testProviders } from '@shared/testing/test-providers';

describe('RedirectGuard', () => {
  const executeGuard: CanMatchFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => RedirectGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
