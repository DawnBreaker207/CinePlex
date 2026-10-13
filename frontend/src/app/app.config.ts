import {
  ApplicationConfig,
  importProvidersFrom,
  inject,
  isDevMode,
  LOCALE_ID,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import {
  provideRouter,
  Router,
  TitleStrategy,
  withComponentInputBinding,
  withInMemoryScrolling,
  withNavigationErrorHandler,
  withPreloading,
  withViewTransitions,
} from '@angular/router';
import { SelectivePreloadingStrategy } from '@core/config/selective-preloading.strategy';
import { routes } from './app.routes';
import { Store } from '@ngrx/store';
import { AuthService } from '@core/auth/auth.service';
import { MovieActions } from '@domain/movie/data-access/movie.actions';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { provideStore } from '@ngrx/store';
import { nzIcons } from './icons-provider';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { vi_VN, provideNzI18n } from 'ng-zorro-antd/i18n';
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
import { provideNzConfig, NzConfig } from 'ng-zorro-antd/core/config';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { NzModalModule } from 'ng-zorro-antd/modal';

const nzConfig: NzConfig = {
  theme: {
    primaryColor: '#e4002b',
  },
  message: {
    nzTop: 72,
  },
  notification: {
    nzTop: 72,
  },
};
import { AuthInterceptor } from '@core/interceptor/auth.interceptor';
import { CacheInterceptor } from '@core/interceptor/cache.interceptor';
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
import { SettingsService } from '@core/services/settings/settings.service';
import { AppTitleStrategy } from '@core/config/title-strategy';
import { provideMarkdown } from 'ngx-markdown';

registerLocaleData(localVi);
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    CurrencyPipe,
    CurrencyFormatPipe,
    provideZonelessChangeDetection(),
    provideHttpClient(
      withInterceptors([
        AuthInterceptor,
        CacheInterceptor,
        ErrorInterceptor,
      ]),
    ),
    provideRouter(
      routes,
      withInMemoryScrolling({
        anchorScrolling: 'enabled',
        scrollPositionRestoration: 'enabled',
      }),
      withComponentInputBinding(),
      withViewTransitions(),
      withPreloading(SelectivePreloadingStrategy),
      withNavigationErrorHandler((error) => {
        console.error('Navigation error:', error);
        inject(Router).navigate(['/errors/500']);
      }),
    ),
    provideNzIcons(nzIcons),
    provideNzI18n(vi_VN),
    provideNzConfig(nzConfig),
    importProvidersFrom(NzModalModule),
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
    provideAppInitializer(() => {
      const store = inject(Store);
      inject(SettingsService);
      store.dispatch(MovieActions.loadMovies({ page: 0, size: 100 }));
      store.dispatch(TheaterActions.loadTheaters({ page: 0, size: 10 }));
      // Restore session after F5: valid httpOnly cookie refetches access token.
      // Fire-and-forget, never blocks boot; guests fail silently by design.
      inject(AuthService).silentRefresh().subscribe({ error: () => {} });
    }),
    provideTranslateService({
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
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    provideMarkdown(),
    provideAnimationsAsync(),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode() }),
  ],
};
