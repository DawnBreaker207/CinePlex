import { Component, inject } from '@angular/core';
import { UpperCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { NzHeaderComponent } from 'ng-zorro-antd/layout';
import { NzMenuDirective } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { AuthService } from '@core/auth/auth.service';
import { ProfileMenuComponent } from '@shared/components/profile-menu/profile-menu.component';
import { SelectShowtimeComponent } from '@features/client/home/components/select/select.component';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { HEADERS } from '@core/constants/column';

@Component({
  selector: 'app-header',
  imports: [
    UpperCasePipe,
    RouterLink,
    NzButtonComponent,
    NzHeaderComponent,
    NzMenuDirective,
    ProfileMenuComponent,
    SelectShowtimeComponent,
    NzIconModule,
    TranslateModule,
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  authService = inject(AuthService);
  private translateService = inject(TranslateService);

  headers = HEADERS;

  useLanguage(lang: string) {
    console.log('Use lang ', lang);
    this.translateService.use(lang);
  }
}
