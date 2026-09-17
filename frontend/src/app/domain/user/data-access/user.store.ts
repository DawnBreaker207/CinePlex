import { User, UserProfile } from '@domain/user/models/user.model';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { UserService } from '@domain/user/data-access/user.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { Pagination } from '@core/models/common.model';
import { errorMessage } from '@shared/utils/error.helper';

export interface UserState {
  users: User[];
  selectedUser: User | null;
  pagination: Pagination | null;
  loading: boolean;
  loadingDetails: boolean;
  saving: boolean;

  error: string | null;
}
export const initialState: UserState = {
  users: [],
  selectedUser: null,
  pagination: null,
  loading: false,
  loadingDetails: false,
  saving: false,
  error: null,
};

export const UserStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    usersCount: computed(() => store.users().length),
    hasUsers: computed(() => store.users().length > 0),
    getUserById: computed(
      () => (id: number) => store.users().find((u) => u.userId === id),
    ),
  })),

  withMethods((store, userService = inject(UserService)) => ({
    setSelectedUser(user: User | null) {
      patchState(store, { selectedUser: user, error: null });
    },
    clearSelectedUser() {
      patchState(store, { selectedUser: null, error: null });
    },
    clearError() {
      patchState(store, { error: null });
    },

    loadUsers: rxMethod<{ page: number; size: number }>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(({ page, size }) =>
          userService.getAll({ page, size }).pipe(
            tapResponse({
              next: ({ content, pagination }) =>
                patchState(store, {
                  users: content,
                  pagination: pagination,
                  loading: false,
                }),
              error: (error) =>
                patchState(store, { error: errorMessage(error), loadingDetails: false }),
            }),
          ),
        ),
      ),
    ),

    loadUser: rxMethod<number>(
      pipe(
        tap(() => patchState(store, { loadingDetails: true, error: null })),
        switchMap((id) =>
          userService.getById(id).pipe(
            tapResponse({
              next: (user) => {
                patchState(store, (state) => {
                  const existed = state.users.findIndex(
                    (u: User) => u.userId === user.userId,
                  );
                  const updatedUsers =
                    existed >= 0
                      ? state.users.map((u) =>
                          u.userId === user.userId ? user : u,
                        )
                      : [...state.users, user];

                  return {
                    users: updatedUsers,
                    selectedUser: user,
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

    updateUserProfile: rxMethod<{ id: number; user: UserProfile }>(
      pipe(
        tap(() => patchState(store, { saving: true, error: null })),
        switchMap(({ id, user }) =>
          userService.updateInfo(id, user).pipe(
            tapResponse({
              next: (updatedUser) => {
                patchState(store, (state) => ({
                  users: state.users.map((u) =>
                    u.userId === updatedUser.userId ? updatedUser : u,
                  ),
                  selectedUser:
                    state.selectedUser?.userId === updatedUser.userId
                      ? updatedUser
                      : state.selectedUser,
                  saving: false,
                }));
              },
              error: (error) =>
                patchState(store, { error: errorMessage(error), loadingDetails: false }),
            }),
          ),
        ),
      ),
    ),

    updateUserStatus: rxMethod<{ id: number; isDeleted: boolean }>(
      pipe(
        tap(({ id, isDeleted }) => {
          patchState(store, (state) => ({
            users: state.users.map((u) =>
              u.userId === id ? { ...u, isDeleted: isDeleted } : u,
            ),
            error: null,
          }));
        }),
        switchMap(({ id, isDeleted }) =>
          userService.updateStatus(id, isDeleted).pipe(
            tapResponse({
              next: () => {},
              error: (error) => {
                patchState(store, (state) => ({
                  users: state.users.map((u) =>
                    u.userId === id ? { ...u, isDeleted: !isDeleted } : u,
                  ),
                  error: errorMessage(error),
                }));
              },
            }),
          ),
        ),
      ),
    ),
  })),
);
