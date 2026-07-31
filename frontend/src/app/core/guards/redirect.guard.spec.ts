import { TestBed } from '@angular/core/testing';
import { CanMatchFn } from '@angular/router';

import { RedirectGuard } from './redirect.guard';

describe('RedirectGuard', () => {
  const executeGuard: CanMatchFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => RedirectGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
