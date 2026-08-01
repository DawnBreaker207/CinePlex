import { createReducer, on } from '@ngrx/store';
import { errorMessage } from '@shared/utils/error.helper';
import { Jwt, RefreshToken } from '@core/models/jwt.model';
import { AuthActions } from '@core/auth/auth.actions';

export const authFeatureKey = 'authKey';

export interface AuthState {
  jwt: Jwt | null;
  token: RefreshToken | null;
  userId: number | null;
  email: string | null;
  loading: boolean;
  error: string | null;
}

export const initialState: AuthState = {
  jwt: null,
  token: null,
  userId: null,
  email: null,
  loading: false,
  error: null,
};

export const authReducer = createReducer(
  initialState,

  // Register
  on(AuthActions.loadRegister, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(AuthActions.loadRegisterSuccess, (state) => ({
    ...state,
    loading: false,
    error: null,
  })),
  on(AuthActions.loadRegisterFailure, (state, { error }) => ({
    ...state,
    error: errorMessage(error),
    loading: false,
  })),

  // Login
  on(AuthActions.loadLogin, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(AuthActions.loadLoginSuccess, (state, { jwt }) => {
    return {
      ...state,
      jwt: jwt,
      email: jwt.email,
      userId: jwt.userId,
      loading: false,
    };
  }),
  on(AuthActions.loadLoginFailure, (state, { error }) => ({
    ...state,
    error: errorMessage(error),
    loading: false,
  })),

  // Logout
  on(AuthActions.loadLogout, (state) => {
    return {
      ...state,
      loading: true,
    };
  }),
  on(AuthActions.loadLogoutSuccess, () => initialState),
  on(AuthActions.loadLogoutFailure, (state, { error }) => {
    return {
      ...state,
      error: errorMessage(error),
      loading: false,
    };
  }),

  //   Refresh Token
  on(AuthActions.loadRefreshTokenSuccess, (state, { token }) => ({
    ...state,
    token,
    jwt: state.jwt
      ? {
          ...state.jwt,
          accessToken: token.accessToken,
        }
      : null,
    loading: false,
    error: null,
  })),
  on(AuthActions.loadRefreshTokenFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error: errorMessage(error),
  })),
);
