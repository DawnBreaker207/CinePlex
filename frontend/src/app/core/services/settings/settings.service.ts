import { inject, Injectable, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { NzI18nService, en_US, vi_VN } from 'ng-zorro-antd/i18n';

export type AppLang = 'vi' | 'en';

const LANG_KEY = 'cineplex.lang';
const THEME_KEY = 'cineplex.theme';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private translate = inject(TranslateService);
  private nzI18n = inject(NzI18nService);

  readonly lang = signal<AppLang>('vi');
  readonly theme = signal(false);

  constructor() {
    this.lang.set(this.readLang());
    this.theme.set(this.readTheme());
    this.apply();
  }

  setLang(lang: AppLang): void {
    this.lang.set(lang);
    localStorage.setItem(LANG_KEY, lang);
    this.apply();
  }

  toggleTheme(): void {
    this.theme.update((t) => !t);
    localStorage.setItem(THEME_KEY, this.theme() ? 'dark' : 'light');
    this.apply();
  }

  private apply(): void {
    this.translate.use(this.lang());
    this.nzI18n.setLocale(this.lang() === 'vi' ? vi_VN : en_US);
    document.documentElement.lang = this.lang();
    document.documentElement.classList.toggle('dark', this.theme());
  }

  private readLang(): AppLang {
    const saved = localStorage.getItem(LANG_KEY);
    return saved === 'en' ? 'en' : 'vi';
  }

  private readTheme(): boolean {
    const saved = localStorage.getItem(THEME_KEY);
    if (saved === 'dark') return true;
    if (saved === 'light') return false;
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}

