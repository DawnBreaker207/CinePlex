import {
  patchState,
  signalStore,
  withMethods,
  withState,
} from '@ngrx/signals';
import { inject } from '@angular/core';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, forkJoin, map, of, pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { TranslateService } from '@ngx-translate/core';
import { User } from '@domain/user/models/user.model';
import { UserService } from '@domain/user/data-access/user.service';
import { PermissionService } from '@domain/permission/data-access/permission.service';
import { PermissionKey } from '@core/constants/enum';
import { errorMessage } from '@shared/utils/error.helper';

const STAFF_ROLES = ['OWNER', 'ADMIN'];

export interface PermissionState {
  staff: User[];
  permsByUser: Record<number, PermissionKey[]>;
  loading: boolean;
  saving: boolean;
  denied: boolean;
  error: string | null;
}

const initialState: PermissionState = {
  staff: [],
  permsByUser: {},
  loading: false,
  saving: false,
  denied: false,
  error: null,
};

export const PermissionStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods(
    (
      store,
      userService = inject(UserService),
      permissionService = inject(PermissionService),
      translate = inject(TranslateService),
    ) => ({
      clearError() {
        patchState(store, { error: null });
      },
      load: rxMethod<void>(
        pipe(
          tap(() => patchState(store, { loading: true, error: null, denied: false })),
          switchMap(() =>
            userService.getAll({ page: 0, size: 1000 }).pipe(
              switchMap(({ content }) => {
                const staff = content.filter((u) => STAFF_ROLES.includes(u.role));
                return forkJoin(
                  staff.map((u) =>
                    permissionService.list(u.userId).pipe(
                      map((keys) => ({ userId: u.userId, keys })),
                      // per-user read failure (e.g. non-owner hitting the owner-only
                      // endpoint) must not render as "no permissions"
                      catchError(() => of({ userId: u.userId, keys: null as PermissionKey[] | null })),
                    ),
                  ),
                ).pipe(
                  tap((results) => {
                    const permsByUser: Record<number, PermissionKey[]> = {};
                    let failures = 0;
                    results.forEach((r) => {
                      if (r.keys) permsByUser[r.userId] = r.keys;
                      else failures++;
                    });
                    // the owner gate is per-caller, so failure is all-or-nothing
                    const denied = results.length > 0 && failures === results.length;
                    patchState(store, {
                      staff,
                      permsByUser,
                      denied,
                      error: denied ? translate.instant('permission.forbidden') : null,
                      loading: false,
                    });
                  }),
                );
              }),
            ),
          ),
          tapResponse({
            next: () => {},
            error: (error) =>
              patchState(store, { error: errorMessage(error), loading: false }),
          }),
        ),
      ),
      toggle: rxMethod<{ userId: number; key: PermissionKey }>(
        pipe(
          tap(() => patchState(store, { saving: true, error: null })),
          switchMap(({ userId, key }) => {
            const has = (store.permsByUser()[userId] ?? []).includes(key);
            const call = has
              ? permissionService.revoke(userId, key)
              : permissionService.grant(userId, key);
            return call.pipe(
              tapResponse({
                next: (keys) =>
                  patchState(store, (state) => ({
                    permsByUser: { ...state.permsByUser, [userId]: keys },
                    saving: false,
                  })),
                error: (error) =>
                  patchState(store, { error: errorMessage(error), saving: false }),
              }),
            );
          }),
        ),
      ),
    }),
  ),
);