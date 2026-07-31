import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn } from '@angular/common/http';

import { AuthInterceptor } from './auth.interceptor';

import { testProviders } from '@shared/testing/test-providers';

describe('AuthInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => AuthInterceptor(req, next));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [testProviders],
    });
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });
});
