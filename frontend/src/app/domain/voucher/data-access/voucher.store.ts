import { Pagination } from '@core/models/common.model';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { finalize, pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import {
  Voucher,
  VoucherCalculate,
  VoucherRequest,
  UserVoucher,
} from '@domain/voucher/model/voucher.model';
import { VoucherService } from '@domain/voucher/data-access/voucher.service';
import { TranslateService } from '@ngx-translate/core';
import { errorMessage } from '@shared/utils/error.helper';

export interface VoucherState {
  vouchers: Voucher[];
  selectVoucher: Voucher | null;
  calculateResult: VoucherCalculate | null;
  userVouchers: UserVoucher[];
  pagination: Pagination | null;
  loading: boolean;
  loadingDetails: boolean;
  saving: boolean;
  error: string | null;
}
export const initialState: VoucherState = {
  vouchers: [],
  selectVoucher: null,
  calculateResult: null,
  userVouchers: [],
  pagination: null,
  loading: false,
  loadingDetails: false,
  saving: false,
  error: null,
};
export const VoucherStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    vouchersCount: computed(() => store.vouchers.length),
    hasVouchers: computed(() => store.vouchers.length > 0),
    getVoucherById: computed(
      () => (id: number) => store.vouchers().find((a) => a.id === id),
    ),
  })),

  withMethods((store, voucherService = inject(VoucherService), translate = inject(TranslateService)) => ({
    setSelectedVoucher(voucher: Voucher | null) {
      patchState(store, { selectVoucher: voucher, error: null });
    },
    //
    clearSelectedVoucher() {
      patchState(store, { selectVoucher: null, error: null });
    },
    //
    clearError() {
      patchState(store, { error: null });
    },
    //
    clearCalculation() {
      patchState(store, { calculateResult: null, loadingDetails: false, error: null });
    },
    //
    loadVouchers: rxMethod<{ page: number; size: number }>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(({ page, size }) =>
          voucherService.getAll({ page, size }).pipe(
            tapResponse({
              next: ({ content, pagination }) =>
                patchState(store, {
                  vouchers: content,
                  pagination: pagination,
                  loading: false,
                }),
              error: (error) =>
                patchState(store, { error: errorMessage(error), loading: false }),
            }),
          ),
        ),
      ),
    ),
    //
    loadVoucher: rxMethod<number>(
      pipe(
        tap(() => patchState(store, { loadingDetails: true, error: null })),
        switchMap((id) =>
          voucherService.getOne(id).pipe(
            tapResponse({
              next: (voucher) => {
                patchState(store, (state) => {
                  const existed = state.vouchers.findIndex(
                    (v: Voucher) => v.id === voucher.id,
                  );
                  const updatedVouchers =
                    existed >= 0
                      ? state.vouchers.map((v) =>
                          v.id === voucher.id ? voucher : v,
                        )
                      : [...state.vouchers, voucher];
                  return {
                    vouchers: updatedVouchers,
                    selectVoucher: voucher,
                    loadingDetails: false,
                  };
                });
              },
              error: (error) =>
                patchState(store, { error: errorMessage(error), loadingDetails: false }),
            }),
          ),
        ),
      ),
    ),
    //
    createVoucher: rxMethod<VoucherRequest>(
      pipe(
        tap(() => patchState(store, { saving: true, error: null })),
        switchMap((voucher) =>
          voucherService.add(voucher).pipe(
            tapResponse({
              next: (voucher) => {
                patchState(store, (state) => ({
                  vouchers: [voucher, ...state.vouchers],
                  selectVoucher: voucher,
                  saving: false,
                }));
              },
              error: (error) =>
                patchState(store, { error: errorMessage(error), saving: false }),
            }),
          ),
        ),
      ),
    ),
    //
    updateVoucher: rxMethod<{ id: number; voucher: VoucherRequest }>(
      pipe(
        tap(() => patchState(store, { saving: true, error: null })),
        switchMap(({ id, voucher }) =>
          voucherService.update(id, voucher).pipe(
            tapResponse({
              next: (voucher) => {
                patchState(store, (state) => ({
                  vouchers: state.vouchers.map((v) =>
                    v.id === voucher.id ? voucher : v,
                  ),
                  selectVoucher: voucher,
                  saving: false,
                }));
              },
              error: (error) =>
                patchState(store, { error: errorMessage(error), saving: false }),
            }),
          ),
        ),
      ),
    ),
    //
    deleteVoucher: rxMethod<number>(
      pipe(
        tap(() => patchState(store, { saving: true, error: null })),
        switchMap((id) =>
          voucherService.delete(id).pipe(
            tapResponse({
              next: () => {
                patchState(store, (state) => ({
                  vouchers: state.vouchers.filter((v) => v.id !== id),
                  selectVoucher: null,
                  saving: false,
                }));
              },
              error: (error) =>
                patchState(store, { error: errorMessage(error), saving: false }),
            }),
          ),
        ),
      ),
    ),
    //
    calculateDiscount: rxMethod<{ code: string; total: number }>(
      pipe(
        tap(() =>
          patchState(store, {
            loadingDetails: true,
            calculateResult: null,
            error: null,
          }),
        ),
        switchMap(({ code, total }) =>
          voucherService.calculateDiscount(code, total).pipe(
            finalize(() => patchState(store, { loadingDetails: false })),
            tapResponse({
              next: (result) => {
                patchState(store, { calculateResult: result });
              },
              error: (error) => {
                const msg = typeof error === 'string' ? error : errorMessage(error) || translate.instant('voucher.invalidCode');
                patchState(store, { error: msg });
              },
            }),
          ),
        ),
      ),
    ),
    //
    loadUserVouchers: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(() =>
          voucherService.getUserVouchers().pipe(
            finalize(() => patchState(store, { loading: false })),
            tapResponse({
              next: (userVouchers) => patchState(store, { userVouchers }),
              error: (error) =>
                patchState(store, { error: errorMessage(error) }),
            }),
          ),
        ),
      ),
    ),
    //
    claimVoucher: rxMethod<string>(
      pipe(
        tap(() => patchState(store, { saving: true, error: null })),
        switchMap((code) =>
          voucherService.claimVoucher(code).pipe(
            finalize(() => patchState(store, { saving: false })),
            tapResponse({
              next: (claimed) =>
                patchState(store, (s) => ({
                  userVouchers: [claimed, ...s.userVouchers],
                })),
              error: (error) =>
                patchState(store, { error: errorMessage(error) }),
            }),
          ),
        ),
      ),
    ),
  })),
);
