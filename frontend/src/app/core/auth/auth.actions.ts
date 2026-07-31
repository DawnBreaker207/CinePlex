import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { LoginRequest, RegisterRequest } from '@domain/user/models/user.model';
import { Jwt, RefreshToken } from '@core/models/jwt.model';

export const AuthActions = createActionGroup({
  source: 'Auth',
  events: {
    'Load Register': props<{ user: RegisterRequest }>(),
    'Load Register Success': props<{ token: string }>(),
    'Load Register Failure': props<{ error: unknown }>(),

    'Load Login': props<{ user: LoginRequest }>(),
    'Load Login Success': props<{ jwt: Jwt }>(),
    'Load Login Failure': props<{ error: unknown }>(),

    'Load Logout': emptyProps(),
    'Load Logout Success': emptyProps(),
    'Load Logout Failure': props<{ error: unknown }>(),

    'Load Refresh Token': emptyProps(),
    'Load Refresh Token Success': props<{ token: RefreshToken }>(),
    'Load Refresh Token Failure': props<{ error: unknown }>(),
  },
});
