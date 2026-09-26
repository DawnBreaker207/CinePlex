import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { PaymentRefund } from '@domain/payment/models/payment.model';
import { PaymentService } from '@domain/payment/data-access/payment.service';
import { ToastService } from '@core/services/toast/toast.service';
import { TranslateService } from '@ngx-translate/core';
import { errorMessage } from '@shared/utils/error.helper';

export interface RefundState {
  refunds: PaymentRefund[];
  loading: boolean;
  saving: boolean;
  error: string | null;
}

const initialState: RefundState = {
  refunds: [],
  loading: false,
  saving: false,
  error: null,
};

export const RefundStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    hasRefunds: computed(() => store.refunds().length > 0),
  })),
  withMethods(
    (
      store,
      paymentService = inject(PaymentService),
      toast = inject(ToastService),
      translate = inject(TranslateService),
    ) => {
    const notify = (titleKey: string, messageKey: string) =>
      toast.createNotification({
        type: 'success',
        title: translate.instant(titleKey),
        message: translate.instant(messageKey),
      });
    return {
    clearError() {
      patchState(store, { error: null });
    },
    loadRefunds: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(() =>
          paymentService.getRefundPending().pipe(
            tapResponse({
              next: (refunds) => patchState(store, { refunds, loading: false }),
              error: (error) =>
                patchState(store, { error: errorMessage(error), loading: false }),
            }),
          ),
        ),
      ),
    ),
    approveRefund: rxMethod<string>(
      pipe(
        tap(() => patchState(store, { saving: true, error: null })),
        switchMap((reservationId) =>
          paymentService.approveRefund(reservationId).pipe(
            tapResponse({
              next: () => {
                patchState(store, (state) => ({
                  refunds: state.refunds.filter(
                    (r) => r.reservationId !== reservationId,
                  ),
                  saving: false,
                }));
                notify('refund.approve', 'refund.approved');
              },
              error: (error) =>
                patchState(store, { error: errorMessage(error), saving: false }),
            }),
          ),
        ),
      ),
    ),
    rejectRefund: rxMethod<string>(
      pipe(
        tap(() => patchState(store, { saving: true, error: null })),
        switchMap((reservationId) =>
          paymentService.rejectRefund(reservationId).pipe(
            tapResponse({
              next: () => {
                patchState(store, (state) => ({
                  refunds: state.refunds.filter(
                    (r) => r.reservationId !== reservationId,
                  ),
                  saving: false,
                }));
                notify('refund.reject', 'refund.rejected');
              },
              error: (error) =>
                patchState(store, { error: errorMessage(error), saving: false }),
            }),
          ),
        ),
      ),
    ),
    };
  }),
);