import { inject, Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Injectable({ providedIn: 'root' })
export class AppTitleStrategy extends TitleStrategy {
  private title = inject(Title);
  private translate = inject(TranslateService);

  override updateTitle(snapshot: RouterStateSnapshot) {
    const title = this.buildTitle(snapshot);
    if (title) {
      this.title.setTitle(`${this.translate.instant(title)} | CinePlex`);
    } else {
      this.title.setTitle('CinePlex');
    }
  }
}
