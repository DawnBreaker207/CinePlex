import {
  ApplicationConfig,
  importProvidersFrom,
  isDevMode,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideStore } from '@ngrx/store';
import { nzIcons } from './icons-provider';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { en_US, provideNzI18n } from 'ng-zorro-antd/i18n';
import {
  CommonModule,
  CurrencyPipe,
  registerLocaleData,
} from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { NgxEchartsModule } from 'ngx-echarts';
import { icons, LucideAngularModule } from 'lucide-angular';
import localVi from '@angular/common/locales/vi';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { AuthInterceptor } from '@core/interceptor/auth.interceptor';
import { ErrorInterceptor } from '@core/interceptor/error.interceptor';
import { authFeatureKey, authReducer } from '@core/auth/auth.reducers';
import {
  movieFeatureKey,
  movieReducer,
} from '@domain/movie/data-access/movie.reducers';
import {
  theaterFeatureKey,
  theaterReducer,
} from '@domain/theater/data-access/theater.reducers';
import {
  showtimeFeatureKey,
  showtimeReducer,
} from '@domain/showtime/data-access/showtime.reducers';
import { AuthEffects } from '@core/auth/auth.effects';
import { MovieEffects } from '@domain/movie/data-access/movie.effects';
import { TheaterEffects } from '@domain/theater/data-access/theater.effects';
import { ShowtimeEffects } from '@domain/showtime/data-access/showtime.effects';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideTranslateService } from '@ngx-translate/core';
import { provideAppSecurity } from '@core/config/app.security';
import { provideMarkdown } from 'ngx-markdown';

registerLocaleData(localVi);
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    CurrencyPipe,
    CurrencyFormatPipe,
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([AuthInterceptor, ErrorInterceptor])),
    provideRouter(routes),
    provideNzIcons(nzIcons),
    provideNzI18n(en_US),
    provideStore({
      [authFeatureKey]: authReducer,
      [movieFeatureKey]: movieReducer,
      [theaterFeatureKey]: theaterReducer,
      [showtimeFeatureKey]: showtimeReducer,
    }),
    provideEffects([
      AuthEffects,
      MovieEffects,
      TheaterEffects,
      ShowtimeEffects,
    ]),
    provideTranslateService({
      lang: 'vi',
      fallbackLang: 'vi',
      loader: provideTranslateHttpLoader({
        prefix: '/i18n/',
        suffix: '.json',
      }),
    }),
    importProvidersFrom(
      NgxEchartsModule.forRoot({
        echarts: () => import('echarts'),
      }),
    ),
    importProvidersFrom(
      CommonModule,
      FormsModule,
      ReactiveFormsModule,
      LucideAngularModule.pick(icons),
    ),
    { provide: LOCALE_ID, useValue: 'vi' },
    provideAppSecurity(),
    provideMarkdown(),
    provideAnimationsAsync(),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode() }),
  ],
};
