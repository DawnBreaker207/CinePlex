import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { NzI18nService } from 'ng-zorro-antd/i18n';

import { SettingsService } from './settings.service';

describe('SettingsService', () => {
  let service: SettingsService;
  let translate: { use: jasmine.Spy };
  let nzI18n: { setLocale: jasmine.Spy };

  beforeEach(() => {
    localStorage.clear();
    translate = { use: jasmine.createSpy('use') };
    nzI18n = { setLocale: jasmine.createSpy('setLocale') };
    TestBed.configureTestingModule({
      providers: [
        { provide: TranslateService, useValue: translate },
        { provide: NzI18nService, useValue: nzI18n },
      ],
    });
    service = TestBed.inject(SettingsService);
  });

  it('should default to vi + light when nothing stored', () => {
    expect(service.lang()).toBe('vi');
    expect(service.theme()).toBe(false);
    expect(translate.use).toHaveBeenCalledWith('vi');
    expect(nzI18n.setLocale).toHaveBeenCalled();
  });

  it('should toggle dark mode and persist', () => {
    service.toggleTheme();
    expect(service.theme()).toBe(true);
    expect(localStorage.getItem('cineplex.theme')).toBe('dark');
    service.toggleTheme();
    expect(service.theme()).toBe(false);
    expect(localStorage.getItem('cineplex.theme')).toBe('light');
  });

  it('should switch language and persist', () => {
    service.setLang('en');
    expect(service.lang()).toBe('en');
    expect(localStorage.getItem('cineplex.lang')).toBe('en');
    expect(translate.use).toHaveBeenCalledWith('en');
  });

  it('should restore stored preferences', () => {
    localStorage.setItem('cineplex.theme', 'dark');
    localStorage.setItem('cineplex.lang', 'en');
    const restored = TestBed.inject(SettingsService);
    expect(restored.theme()).toBe(true);
    expect(restored.lang()).toBe('en');
  });
});


