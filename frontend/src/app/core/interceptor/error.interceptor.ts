import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, retry, throwError, timer } from 'rxjs';
import { GlobalErrorService } from '@core/services/error/global-error.service';
import { SKIP_ERROR_NOTIFICATION } from '@core/constants/http-context.tokens';

export const ErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const errorService = inject(GlobalErrorService);
  const { maxRetries } = errorService.RETRY_CONFIG;

  // Retry is safe only for idempotent reads — never replay POST/PUT/DELETE
  // (payment, refund, check-in) where a replay can double-charge or double-apply.
  const withRetry =
    req.method === 'GET'
      ? next(req).pipe(
          retry({
            count: maxRetries,
            delay: (error: HttpErrorResponse, retryCount) => {
              if (errorService.shouldRetry(error.status)) {
                const delayTime = errorService.getRetryDelay(retryCount);

                return timer(delayTime);
              }
              return throwError(() => error);
            },
          }),
        )
      : next(req);

  return withRetry.pipe(
    catchError((error: HttpErrorResponse) => {
      const shouldSkip = req.context.get(SKIP_ERROR_NOTIFICATION);
      errorService.handleError(error, shouldSkip);
      return throwError(() => error);
    }),
  );
};
