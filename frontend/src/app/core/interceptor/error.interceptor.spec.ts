import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn } from '@angular/common/http';

import { ErrorInterceptor } from './error.interceptor';

import { testProviders } from '@shared/testing/test-providers';

describe('ErrorInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => ErrorInterceptor(req, next));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });
});
