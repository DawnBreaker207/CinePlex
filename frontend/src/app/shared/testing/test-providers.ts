import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideMockStore } from '@ngrx/store/testing';
import { ScannedActionsSubject } from '@ngrx/store';
import { ActivatedRoute } from '@angular/router';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { NzModalRef, NZ_MODAL_DATA } from 'ng-zorro-antd/modal';
import { NzDrawerRef, NZ_DRAWER_DATA } from 'ng-zorro-antd/drawer';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { nzIcons } from '../../icons-provider';
import { icons, LUCIDE_ICONS, LucideIconProvider } from 'lucide-angular';
import { NGX_ECHARTS_CONFIG } from 'ngx-echarts';
import { of, Subject } from 'rxjs';

export const testProviders = [
  provideHttpClient(),
  provideHttpClientTesting(),
  provideNoopAnimations(),
  provideMockStore({
    initialState: {
      movieKey: { movies: [], selectedMovie: null, pagination: null, loading: false, loadingDetails: false, saving: false, searchQuery: null, error: null },
      theaterKey: { theaters: [], selectedTheater: null, selectedTheaterId: null, pagination: null, loading: false, loadingDetails: false, saving: false, error: null },
      showtimeKey: { showtimes: [], selectedShowtime: null, pagination: null, movieId: null, theaterId: null, loading: false, loadingDetails: false, saving: false, error: null },
      authKey: { jwt: null, token: null, userId: null, email: null, loading: false, error: null },
    },
  }),
  provideNzIcons(nzIcons),
  { provide: LUCIDE_ICONS, multi: true, useValue: new LucideIconProvider(icons) },
  { provide: NGX_ECHARTS_CONFIG, useValue: { renderer: 'svg' } },
  { provide: ScannedActionsSubject, useValue: new ScannedActionsSubject() },
  { provide: DatePipe, useValue: { transform: (value: unknown) => value } },
  { provide: CurrencyPipe, useValue: new CurrencyPipe('en-US') },
  {
    provide: TranslateService,
    useValue: {
      instant: (key: string) => key,
      get: (key: string) => of(key),
      stream: of(''),
      currentLang: 'vi',
      getCurrentLang: () => 'vi',
      getFallbackLang: () => 'vi',
      addLangs: () => {},
      setDefaultLang: () => {},
      use: () => of(''),
      onTranslationChange: new Subject(),
      onLangChange: new Subject(),
      onDefaultLangChange: new Subject(),
      onFallbackLangChange: new Subject(),
    },
  },
  { provide: NzModalRef, useValue: { close: () => {}, updateConfig: () => {}, getConfig: () => ({ nzData: { showtimes: [] } }), setConfig: () => {}, getContentComponent: () => null, triggerOk: () => {}, triggerCancel: () => {} } },
  { provide: NZ_MODAL_DATA, useValue: {} },
  { provide: NzDrawerRef, useValue: { close: () => {}, updateConfig: () => {} } },
  { provide: NZ_DRAWER_DATA, useValue: {} },
  { provide: ActivatedRoute, useValue: { snapshot: { data: {}, children: [] }, root: { children: [] }, queryParams: of({}), params: of({}) } },
];
